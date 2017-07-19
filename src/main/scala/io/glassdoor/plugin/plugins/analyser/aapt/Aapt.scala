package io.glassdoor.plugin.plugins.analyser.aapt

import java.io.{BufferedWriter, File, FileWriter}

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.mutable.ArrayBuffer

/**
  * Plugin to interact with the aapt utility found in the Android build tools.
  * Allows to extract data from APKs, like the package name, resources, xml, permissions, etc.
  * Created by Florian Schrofner on 1/29/17.
  */
class Aapt extends Plugin{

  var mResult : Option[Map[String,String]] = None


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    DynamicValues(uniqueId, None, Some(Array[String](parameters(parameters.length - 1))))
  }

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    val androidSdkPath = data.get(ContextConstant.FullKey.ConfigAndroidSdkPath)
    val workingDirectory = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(androidSdkPath.isDefined && workingDirectory.isDefined && parameters != null){
      val destContext = parameters(parameters.length - 1)
      val file = new File(androidSdkPath.get)

      //get build-tools directory
      val buildTools = file.listFiles().filter(e => e.isDirectory && e.getName == "build-tools")

      if(buildTools != null && buildTools.length > 0){
        val aaptPath = getAaptPath(buildTools(0))
        if(aaptPath.isDefined){
          val options = parseCommandlineOptions(parameters)
          if(options.isDefined){
            val apkPath = data.get(ContextConstant.FullKey.OriginalBinaryApk)

            if(apkPath.isDefined){
              val resultData = runAaptCommand(aaptPath.get, apkPath.get, options.get)

              if(resultData.isDefined){
                //TODO: extract subdir name from context to save

                val destPath = workingDirectory.get + File.separator + ContextConstant.Key.Aapt + File.separator + destContext.split(Constant.Regex.DescriptorSplitRegex)(1) + File.separator + "result.log"
                val outputFile = new File(destPath)
                outputFile.getParentFile.mkdirs()

                val writer = new BufferedWriter(new FileWriter(outputFile, true))
                writer.write(resultData.get)
                writer.close()

                val result = Map[String, String](destContext -> outputFile.getAbsolutePath)
                mResult = Some(result)
              } else {
                Log.debug("error when executing command!")
              }
            }
          }
        }
      }
    }

    ready()
  }

  def runAaptCommand(aaptPath : String, apkPath: String, aaptOptions: AaptOptions): Option[String] ={
    val command = ArrayBuffer[String]()
    command.append(aaptPath)
    if(aaptOptions.command == AaptCommand.Dump){
      command.append("dump")

      aaptOptions.dumpType match {
        case DumpType.Strings =>
          command.append("strings")
        case DumpType.Badging =>
          command.append("badging")
        case DumpType.Permissions =>
          command.append("permissions")
        case DumpType.Resources =>
          command.append("resources")
        case DumpType.Configurations =>
          command.append("configurations")
        case DumpType.XmlTree =>
          command.append("xmltree")
        case DumpType.XmlStrings =>
          command.append("xmlstrings")
      }
    }

    command.append(apkPath)

    Log.debug("executing command: " + command)

    val executor = new SystemCommandExecutor()
    executor.executeSystemCommand(command)
  }

  def parseCommandlineOptions(parameters: Array[String]): Option[AaptOptions] ={
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){
      val options = new AaptOptions()
      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            parameter.name.toLowerCase match {
              case "dump" =>
                options.command = AaptCommand.Dump
              case "strings" =>
                options.dumpType = DumpType.Strings
              case "badging" =>
                options.dumpType = DumpType.Badging
              case "permissions" =>
                options.dumpType = DumpType.Permissions
              case "resources" =>
                options.dumpType = DumpType.Resources
              case "configurations" =>
                options.dumpType = DumpType.Configurations
              case "xmltree" =>
                options.dumpType = DumpType.XmlTree
              case "xmlstrings" =>
                options.dumpType = DumpType.XmlStrings
              case _ =>
                //ignore
            }
            //TODO: add other commands, flags and named parameters
        }
      }
      return Some(options)
    }
    None
  }

  def getAaptPath(buildTools : File): Option[String] ={
    var aaptPath = ""

    if(buildTools != null && buildTools.listFiles() != null && buildTools.listFiles().length > 0){
      if(buildTools.listFiles().length > 1){
        Log.debug("there are multiple build tools installed")
        var newestBuildTools : File = null
        val highestNumber = Array(0,0,0)

        for(file <- buildTools.listFiles()){
          Log.debug("checking build tools: " + file.getName)
          if(file.isDirectory){
            Log.debug("file is directory: " + file.getName)
            val versionNumber = file.getName.split("""\.""")
            Log.debug("size after splitting: " + versionNumber.length)

            if(versionNumber != null && versionNumber.length == 3){
              //split file name, compare each number of version
              if(Integer.valueOf(versionNumber(0)) > highestNumber(0)){
                Log.debug("found newer build tools: " + file.getName)
                highestNumber(0) = Integer.valueOf(versionNumber(0))
                highestNumber(1) = Integer.valueOf(versionNumber(1))
                highestNumber(2) = Integer.valueOf(versionNumber(2))
                newestBuildTools = file
              } else if(Integer.valueOf(versionNumber(0)) == highestNumber(0)){
                if(Integer.valueOf(versionNumber(1)) > highestNumber(1)){
                  Log.debug("found newer build tools: " + file.getName)
                  highestNumber(0) = Integer.valueOf(versionNumber(0))
                  highestNumber(1) = Integer.valueOf(versionNumber(1))
                  highestNumber(2) = Integer.valueOf(versionNumber(2))
                  newestBuildTools = file
                } else if(Integer.valueOf(versionNumber(1)) == highestNumber(1)){
                  if(Integer.valueOf(versionNumber(2)) > highestNumber(2)){
                    Log.debug("found newer build tools: " + file.getName)
                    highestNumber(0) = Integer.valueOf(versionNumber(0))
                    highestNumber(1) = Integer.valueOf(versionNumber(1))
                    highestNumber(2) = Integer.valueOf(versionNumber(2))
                    newestBuildTools = file
                  }
                }
              }
            }
          } else {
            Log.debug("file is no directory")
          }
        }

        if(newestBuildTools != null){
          Log.debug("newest sdk: " + newestBuildTools.getName)
          aaptPath = newestBuildTools.getAbsolutePath + File.separator + "aapt"
          return Some(aaptPath)
        } else {
          Log.debug("error: file is null!")
        }
      } else {
        Log.debug("there is only one build tools version installed")
        aaptPath = buildTools.listFiles()(0).getAbsolutePath + "aapt"
        return Some(aaptPath)
      }
    }

    None
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
