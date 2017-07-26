package io.glassdoor.plugin.plugins.analyser.regex

import java.io.{BufferedWriter, File, FileWriter}

import io.glassdoor.application._
import io.glassdoor.plugin.plugins.analyser.regex.RegexSearchBackend.RegexSearchBackend
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * Runs a regex search with the specified regex or input files over the files inside the specified location.
  * The backend for the search can be selected via flags.
  * Stores the matching lines in a log file in the specified destination.
  * Created by Florian Schrofner on 3/30/16.
  */
class RegexAnalyser extends Plugin{

  var mResult:Option[Map[String,String]] = None
  val mRegexOptions:RegexOptions = new RegexOptions
  var mBufferedWriter:Option[BufferedWriter] = None


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if(parameters.length == 3){
      return DynamicValues(uniqueId, Some(Array[String](parameters(1))), Some(Array[String](parameters(2))))
    } else {
      val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

      var source:Option[String] = None
      var destination:Option[String] = None
      val dependencyBuffer = ArrayBuffer[String]()

      if(parameterArray.isDefined) {
        for(parameter <- parameterArray.get){
          if(parameter.paramType == ParameterType.Parameter){
            if(source.isEmpty){
              source = Some(parameter.name)
            } else if(destination.isEmpty){
              destination = Some(parameter.name)
            }
          }
          if(parameter.paramType == ParameterType.NamedParameter){
            if(parameter.name == "input" | parameter.name == "i"){
              if(parameter.value.isDefined){
                dependencyBuffer.append(parameter.value.get)
              }
            }
          }
        }
      }

      var changes:Option[Array[String]] = None

      if(source.isDefined){
        dependencyBuffer.append(source.get)
      }

      val dependencies = Some(dependencyBuffer.toArray)

      if(destination.isDefined){
        changes = Some(Array[String](destination.get))
      }

      DynamicValues(uniqueId, dependencies, changes)
    }
  }

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    try {
      if(parameters.length == 3){
        Log.debug("no additional parameters, calling regex search")

        //shortest possible call
        val regex = parameters(0)
        val src = parameters(1)
        val dest = parameters(2)

        showEndlessProgress()
        callSearchWithRegex(regex,src,dest, data)
      } else if(parameters.length > 3){
        Log.debug("additional parameters specified, need to parse..")

        //convert to parameter keymap and iterate
        val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

        if(parameterArray.isDefined){

          showEndlessProgress()
          Log.debug("successfully parsed parameter array, size: " + parameterArray.get.length)

          var inputOpt:Option[Array[String]] = None
          var inputSubFilesOpt:Option[mutable.MultiMap[String,String]] = None

          val inputFiles = mutable.ArrayBuffer.empty[String]
          val inputSubFiles = new collection.mutable.HashMap[String, collection.mutable.Set[String]]() with collection.mutable.MultiMap[String, String]

          var srcOpt:Option[String] = None
          var destOpt:Option[String] = None

          for(parameter <- parameterArray.get){
            if(parameter.paramType == ParameterType.NamedParameter){
              parameter.name match {
                case "input" | "i" =>
                  Log.debug("found input tag")
                  if(parameter.value.isDefined){
                    inputFiles += parameter.value.get
                  }
                case "subfile" | "s" =>
                  Log.debug("found subfile tag")
                  if(parameter.value.isDefined){
                    inputSubFiles.addBinding(inputFiles.last, parameter.value.get)
                  }
                case "regex" | "r" =>
                  Log.debug("found regex tag")
                  if(parameter.value.isDefined){
                    mRegexOptions.singleRegex = parameter.value
                  }
                case _ =>
                  Log.debug("error: unrecognised parameter!")
              }
            } else if(parameter.paramType == ParameterType.Flag){
              parameter.name match {
                case "only-matching" | "o" =>
                  mRegexOptions.onlyMatching = true
                case "no-filename" | "h" =>
                  mRegexOptions.noFileName = true
                case "line-number" | "n" =>
                  mRegexOptions.showLineNumber = true
                case "ignore-case" | "i" =>
                  mRegexOptions.ignoreCase = true
                case "print-headers" | "j" =>
                  mRegexOptions.printHeaders = true
                case "fixed-strings" | "F" =>
                  mRegexOptions.patternMatcher = PatternMatcher.Strings
                case "silver-searcher" | "S" =>
                  mRegexOptions.searchBackend = RegexSearchBackend.TheSilverSearcher
                case "overwrite" | "w" =>
                  mRegexOptions.overwrite = true
                case _ =>
                  Log.debug("error: unrecognised flag!")
              }
            } else if(parameter.paramType == ParameterType.Parameter){
              //save src at first and dest at second occurrence
              if(srcOpt.isEmpty){
                srcOpt = Some(parameter.name)
              } else if(destOpt.isEmpty){
                destOpt = Some(parameter.name)
              } else {
                Log.debug("error: trailing parameter")
              }
            }

          }

          if(inputFiles.length > 0){
            Log.debug("input files were specified")
            inputOpt = Some(inputFiles.toArray)
          }

          if(inputSubFiles.size > 0) {
            inputSubFilesOpt = Some(inputSubFiles)
          }

          if(srcOpt.isDefined && destOpt.isDefined && inputOpt.isDefined){
            callSearchWithInputArray(inputOpt.get,inputSubFilesOpt,srcOpt.get,destOpt.get,data)
          } else if(srcOpt.isDefined && destOpt.isDefined && mRegexOptions.singleRegex.isDefined){
            callSearchWithRegex(mRegexOptions.singleRegex.get, srcOpt.get, destOpt.get, data)
          } else {
            setErrorMessage("error: either src, dest or input not defined!")
          }
        } else {
          setErrorMessage("parameters could not be parsed!")
        }
      } else {
        setErrorMessage("error: not enough arguments")
        mResult = None
      }
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        setErrorMessage("error: array index out of bounds")
        mResult = None
      case e: Exception =>
        setErrorMessage("error: other exception")
        e.printStackTrace()
    } finally {
      if(mBufferedWriter.isDefined){
        mBufferedWriter.get.close()
      }
      ready()
    }

  }

  def buildCommand(regex:String, srcPath:String): Seq[String] ={
    if(mRegexOptions.searchBackend == RegexSearchBackend.Grep){
      buildGrepCommand(regex, srcPath)
    } else {
      buildSilverSearcherCommand(regex,srcPath)
    }
  }

  def buildGrepCommand(regex:String, srcPath:String): Seq[String] ={
    val command = ArrayBuffer[String]()
    command.append("grep")
    command.append("-a")
    command.append("-r")

    mRegexOptions.patternMatcher match {
      case PatternMatcher.Basic =>
        command.append("-G")
      case PatternMatcher.Perl =>
        command.append("-P")
      case PatternMatcher.Strings =>
        command.append("-F")
      case _ =>
        command.append("-E")
    }

    if(mRegexOptions.onlyMatching) command.append("-o")
    if(mRegexOptions.noFileName) command.append("-h")
    if(mRegexOptions.showLineNumber) command.append("-n")
    if(mRegexOptions.ignoreCase) command.append("-i")

    command.append(regex)
    command.append(srcPath)
    command
  }

  def buildSilverSearcherCommand(regex:String, srcPath:String):Seq[String] = {
    val command = ArrayBuffer[String]()
    command.append("ag")
    command.append("--nocolor")
    command.append("--search-binary")
    command.append("-a")

    mRegexOptions.patternMatcher match {
      case PatternMatcher.Strings =>
        command.append("-F")
      case _ =>
        //do nothing
    }

    if(mRegexOptions.onlyMatching) command.append("-o")
    if(mRegexOptions.ignoreCase) command.append("-i")


    if(mRegexOptions.showLineNumber){
      command.append("--numbers")
    } else {
      command.append("--nonumbers")
    }

    if(mRegexOptions.noFileName){
      command.append("--nofilename")
    } else {
      command.append("--vimgrep")
    }

    command.append(regex)
    command.append(srcPath)
    command
  }

  def callSearchWithRegex(regex:String, src:String, dest:String, data:Map[String,String]): Unit ={
    Log.debug("calling regex search with regex..")

    val srcPath = data.get(src)
    val workingDirectory = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(srcPath.isDefined && workingDirectory.isDefined){
      val destPath = workingDirectory.get + File.separator + ContextConstant.Key.Regex + File.separator + splitDescriptor(dest)(1) + File.separator + "result.log"
      val outputFile = new File(destPath)
      outputFile.getParentFile.mkdirs()

      val command = buildCommand(regex, srcPath.get)

      Log.debug("issuing command: " + command.toString())

      val executor = new SystemCommandExecutor
      val commandResult = executor.executeSystemCommand(command)

      if(commandResult.isDefined){
        val output = commandResult.get

        Log.debug("regex output" + output)

        if(mBufferedWriter.isEmpty){
          if(!mRegexOptions.overwrite){
            val writer = new BufferedWriter(new FileWriter(outputFile, true))
            mBufferedWriter = Some(writer)
          } else {
            val writer = new BufferedWriter(new FileWriter(outputFile))
            mBufferedWriter = Some(writer)
          }
        }

        val bw = mBufferedWriter.get
        if(mRegexOptions.printHeaders){
          bw.write(RegexAnalyserConstant.HeaderLine.format(regex, src))
          bw.write(RegexAnalyserConstant.NewLine)
        }

        bw.write(output)

        if(mRegexOptions.printHeaders){
          bw.write(RegexAnalyserConstant.NewLine)
        }

        Log.debug("regex finished, saved log to: " + outputFile.getAbsolutePath)

        //only save parent directory, not the exact file
        val result = HashMap[String,String](dest -> outputFile.getParent)
        mResult = Some(result)
      } else {
        setErrorMessage("error: no results when issuing regex search")
        val resultCode = executor.getResultCode
        val error = executor.getErrorOutput
        if(resultCode.isDefined){
          Log.debug("result code: " + resultCode.get)
          if(resultCode.get == 1 && mRegexOptions.searchBackend == RegexSearchBackend.Grep){
            Log.debug("was issuing grep command, result code of 1 means no lines were selected")
            //with grep result code of 1 means, that no lines were selected (see documentation)
            //TODO: check for other backends
            val result = HashMap[String,String](dest -> outputFile.getParent)
            mResult = Some(result)
          } else {
            Log.debug("result code of 1 equals an error!")
          }
        } else {
          Log.debug("no result code")
        }
        if(error.isDefined){
          Log.debug("error output: " + error.get)
        } else {
          Log.debug("no error output")
        }
      }
    } else {
      setErrorMessage("error: either src path or working directory are not defined!")
    }
  }

  def callSearchWithInputFile(inputFilePath:String, src:String, dest:String, data:Map[String,String]):Unit = {
    Log.debug("calling regex with input file: " + inputFilePath)
    val inputFile = new File(inputFilePath)

    if(inputFile.exists() && inputFile.isDirectory){
      //recursively call method
      val subFiles = inputFile.list()

      if(subFiles.length > 0){
        for(file <- subFiles){
          callSearchWithInputFile(file, src, dest, data)
        }
      } else {
        Log.debug("directory " + inputFilePath + " is empty!")
      }
    } else if(inputFile.exists()){
      for (line <- Source.fromFile(inputFile).getLines()) {
        callSearchWithRegex(line, src, dest, data)
      }
    } else {
      setErrorMessage("error: input file not found!")
    }
  }

  def callSearchWithInputArray(inputs:Array[String], inputSubFiles:Option[mutable.MultiMap[String,String]], src:String, dest:String, data:Map[String,String]):Unit = {
    Log.debug("calling regex with input array..")

    for(input <- inputs){
      Log.debug("trying to get value for: " + input)

      val path = data.get(input)

      if(path.isDefined){
        if(inputSubFiles.isDefined && inputSubFiles.get.contains(input)){
          Log.debug("subfiles were specified for: " + input)
          //get all folders that were specified for that input
          val subFilePaths = inputSubFiles.get.get(input).get //this is crazy

          for(subFile <- subFilePaths){
            val inputFilePath = path.get + File.separator + subFile
            //TODO: maybe split destinations up to differentiate inputs?
            callSearchWithInputFile(inputFilePath, src, dest, data)
          }

        } else {
          Log.debug("no subfiles specified for: " + input)
          callSearchWithInputFile(path.get, src, dest, data)
        }
      } else {
        Log.debug("path was not handed over in data")
        setErrorMessage("error: input path is not defined in data")
      }
    }
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DescriptorSplitRegex)
  }

  override def result: Option[Map[String,String]] = {
    mResult
  }

}



object RegexAnalyserConstant {
  val NewLine = sys.props("line.separator")
  val HeaderPrefix = "####################"
  val HeaderPostfix = "####################"
  val HeaderLine = HeaderPrefix + " %s at %s " + HeaderPostfix + NewLine
}