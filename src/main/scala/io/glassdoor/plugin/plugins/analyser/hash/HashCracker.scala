package io.glassdoor.plugin.plugins.analyser.hash

import java.io.{BufferedWriter, File, FileWriter}

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * Created by Florian Schrofner on 7/5/16.
  */
class HashCracker extends Plugin{
  var mHashCrackerOptions = new HashCrackerOptions
  var mResult:Option[Map[String,String]] = None


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)
    if(parameterArray.isDefined){
      applyParameters(parameterArray.get)
      //if hash is not directly provided, it needs to be loaded
      if(mHashCrackerOptions.dictionary.isDefined && !mHashCrackerOptions.singleHash && mHashCrackerOptions.hash.isDefined){
        return DynamicValues(uniqueId, Some(Array[String](mHashCrackerOptions.dictionary.get, mHashCrackerOptions.hash.get)),None)
      } else if(mHashCrackerOptions.dictionary.isDefined && mHashCrackerOptions.singleHash){
        return DynamicValues(uniqueId, Some(Array[String](mHashCrackerOptions.dictionary.get)),None)
      }
    }
    DynamicValues(uniqueId, Some(Array[String]()), None)
  }

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    Log.debug("apply hashcrack called!")
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){
      applyParameters(parameterArray.get)

      if(mHashCrackerOptions.dictionary.isDefined && mHashCrackerOptions.hash.isDefined){
        Log.debug("dictionary and hash are defined in options")

        Log.debug("dictionary: " + mHashCrackerOptions.dictionary.get)
        Log.debug("hash: " + mHashCrackerOptions.hash.get)

        val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)
        var dictPath = data.get(mHashCrackerOptions.dictionary.get)
        var hash : Option [String] = None

        if(mHashCrackerOptions.singleHash) {
          Log.debug("single hash")

          //move hash to file, as john can only work with files
          if(workingDir.isDefined){
            val destPath = workingDir.get + File.separator + ContextConstant.Key.HashCrack + File.separator + "single.hash"
            val outputFile = new File(destPath)
            outputFile.getParentFile.mkdirs()

            //write the hash
            val bw = new BufferedWriter(new FileWriter(outputFile, true))
            bw.write(mHashCrackerOptions.hash.get)
            bw.close()

            hash = Some(destPath)
          }

        } else {
          Log.debug("directory defined")
          hash = data.get(mHashCrackerOptions.hash.get)
        }

        Log.debug("dictionary path: " + dictPath)
        Log.debug("hash: " + hash)

        if(dictPath.isDefined && hash.isDefined){
          Log.debug("dictPath and hash are defined")
          //TODO: check if there are more files in that directory

          //only use subfile, if one is defined
          if(mHashCrackerOptions.dictionarySubFile.isDefined){
            dictPath = Some(dictPath.get + File.separator + mHashCrackerOptions.dictionarySubFile.get)
          }

          val dictFile = new File(dictPath.get)

          showEndlessProgress()

          //if dictionary file is a directory and there is only one file inside that directory
          if(dictFile.isDirectory && dictFile.list.length == 1){
            dictPath = Some(dictFile.listFiles()(0).getAbsolutePath)
          } else if(dictFile.isDirectory){
            setErrorMessage("error: dictionary not clearly specified!")
            //abort
            ready()
            return
          }

          val hashFile = new File(hash.get)

          //if hash directly specifies the path, no need to find the file
          if(hashFile.isDirectory && hashFile.list().length == 1){
            hash = Some(hashFile.listFiles()(0).getAbsolutePath)
          }

          Log.debug("building command with dict: " + dictPath.get +" and hash: " + hash.get)
          val command = buildHashCrackCommand(dictPath.get, hash.get,data)
          Log.debug("hashcrack command: " + command)
          val executor = new SystemCommandExecutor
          var resultString = executor.executeSystemCommand(command)
          val resultCode = executor.getResultCode

          if(resultCode.isDefined && resultCode.get == 0 && workingDir.isDefined){

            Log.debug("command executed successfully, result will be saved..")


            //john does not provide all results in output, extract them from the pot file
            if(mHashCrackerOptions.hashCrackerBackend == HashCrackerBackend.John){
              val stringBuilder = new StringBuilder
              for (line <- Source.fromFile(hash.get).getLines()) {
                val filterCommand = ArrayBuffer[String]()
                filterCommand.append("grep")
                filterCommand.append("-E")
                filterCommand.append("-o")
                filterCommand.append(line + ":.+")
                filterCommand.append(workingDir.get + File.separator + ContextConstant.Key.HashCrack + File.separator + "result.pot")
                val filteredData = executor.executeSystemCommand(filterCommand)
                if(filteredData.isDefined){
                  stringBuilder.append(filteredData.get + sys.props("line.separator"))
                }
              }
              resultString = Some(stringBuilder.toString())
            }

            if(workingDir.isDefined && resultString.isDefined){
              val resultFile = writeResultToFileSystem(workingDir.get, resultString.get)
              val file = new File(resultFile)
              val resultMap = HashMap[String,String](ContextConstant.FullKey.ResultLogHashCrack -> file.getParent)
              mResult = Some(resultMap)
            } else {
              setErrorMessage("error: working dir or result not defined!")
            }
          } else {
            setErrorMessage("error: execution of command caused error!")
            Log.debug("result code:" + executor.getResultCode)
            Log.debug("error output: " +executor.getErrorOutput)
          }
        } else {
          setErrorMessage("error: dictPath and hash are NOT defined after replacement")
        }
      } else {
        setErrorMessage("error: dictionary and hash are NOT defined")
      }

      //TODO: choose correct destination path in working directory
      //TODO: save to result-log.cracked-hashes
    }

    ready()
  }

  def writeResultToFileSystem(workingDir:String, result:String):String = {
    val destPath = workingDir + File.separator + ContextConstant.Key.HashCrack + File.separator + "/result.log"
    val outputFile = new File(destPath)
    outputFile.getParentFile.mkdirs()

    //write the resulting log
    val bw = new BufferedWriter(new FileWriter(outputFile, true))
    bw.write(result)
    bw.close()

    destPath
  }

  def applyParameters(parameterArray:Array[Parameter]):Unit = {
    for(parameter <- parameterArray){
      parameter.paramType match {
        case ParameterType.NamedParameter =>
          parameter.name match {
            case "format" | "f" =>
              if(parameter.value.isDefined){
                parameter.value.get match {
                  case "md5" =>
                    mHashCrackerOptions.hashAlgorithm = Some(HashAlgorithm.Md5)
                  case "sha256" =>
                    mHashCrackerOptions.hashAlgorithm = Some(HashAlgorithm.Sha256)
                }
              }
            case "dictionary-file" | "d" =>
              mHashCrackerOptions.dictionarySubFile = parameter.value
            case _ =>
            //TODO: save named parameters here
          }
        case ParameterType.Flag =>
          parameter.name match {
            case "single-hash" | "s" =>
              mHashCrackerOptions.singleHash = true
            case _ =>
            //TODO: handle flags here
          }

        case ParameterType.Parameter =>
          if(mHashCrackerOptions.dictionary.isEmpty){
            mHashCrackerOptions.dictionary = Some(parameter.name)
          } else if(mHashCrackerOptions.hash.isEmpty){
            mHashCrackerOptions.hash = Some(parameter.name)
          }
      }
    }
  }

  def buildHashCrackCommand(dictionaryFile:String, hashFile:String, data: Map[String, String]):Seq[String] = {
    mHashCrackerOptions.hashCrackerBackend match {
      case HashCrackerBackend.HashCat =>
        //TODO:
        Seq[String]()
      case _ =>
        buildJohnTheRipperCommand(dictionaryFile, hashFile, data)
    }
  }

  def buildJohnTheRipperCommand(dictionaryFile:String, hash:String, data: Map[String, String]):Seq[String] = {
    val command = ArrayBuffer[String]()
    command.append("john")

    val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(workingDir.isDefined){
      val potPath = workingDir.get + File.separator + ContextConstant.Key.HashCrack + File.separator + "result.pot"
      val file = new File(potPath)
      file.getParentFile.mkdirs()
      file.createNewFile()
      command.append("--pot=" + potPath)
    }

    command.append("--wordlist")
    command.append(dictionaryFile)

    if(mHashCrackerOptions.hashAlgorithm.isDefined){
      val stringBuilder = new StringBuilder()
      stringBuilder.append("--format=")

      //convert to john formats
      mHashCrackerOptions.hashAlgorithm.get match {
        case HashAlgorithm.Md5 =>
          stringBuilder.append("Raw-MD5")
        case HashAlgorithm.Sha256 =>
          stringBuilder.append("Raw-SHA256")

      }
      command.append(stringBuilder.toString())
    }

    command.append(hash)

    command
  }

  /**
    * This will be called once you call the ready() method inside your plugin.
    * Please return ALL the changed values here as a map containing the key and the changed value.
    * If you did not change any values, simply return an empty map = Some(Map[String,String]())
    * Returning None here, will be interpreted as an error.
    *
    * @return a map containing all the changed values.
    */
  override def result: Option[Map[String, String]] = {
    mResult
  }

}
