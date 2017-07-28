package io.glassdoor.plugin.plugins.emulator.adb

import java.io.File

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._

/**
  * Created by Florian Schrofner on 3/9/17.
  */
class AdbPull extends Plugin {
  var mResult : Option[Map[String,String]] = None
  var mFixedPermissionCount = 0

  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    //TODO: move to own method for better reusability
    var srcContext : Option[String] = None
    var destContext : Option[String] = None
    var fileToPull : Option[String] = None


    if(parameterArray.isDefined){
      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            if(srcContext.isEmpty && fileToPull.isEmpty){
              srcContext = Some(parameter.name)
            } else {
              destContext = Some(parameter.name)
            }
          case ParameterType.NamedParameter =>
            parameter.name match {
              case "f" =>
                fileToPull = parameter.value
            }
        }
      }

      if(srcContext.isDefined && destContext.isDefined){
        return DynamicValues(uniqueId, Some(Array[String](srcContext.get)), Some(Array[String](destContext.get)))
      } else if(srcContext.isDefined){
        return DynamicValues(uniqueId, Some(Array[String](srcContext.get)), Some(Array[String]()))
      }
    }

    DynamicValues(uniqueId, None, None)
  }

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    Log.debug("adb-pull called")

    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){

      var srcContext : Option[String] = None
      var destContext : Option[String] = None
      var fileToPull : Option[String] = None
      var subFile : Option[String] = None


      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            if(srcContext.isEmpty && fileToPull.isEmpty){
              srcContext = Some(parameter.name)
            } else {
              destContext = Some(parameter.name)
            }
          case ParameterType.NamedParameter =>
            parameter.name match {
              case "f" =>
                fileToPull = parameter.value
              case "s" =>
                subFile = parameter.value
            }
        }
      }

      val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

      if(workingDir.isDefined && destContext.isDefined){

        val destSubDir = splitDescriptor(destContext.get)(1)

        val destDir = workingDir.get + File.separator + ContextConstant.Key.Pull + File.separator + destSubDir
        val destDirFile = new File(destDir)
        destDirFile.mkdirs()

        if(srcContext.isDefined){
          val srcPath = data.get(srcContext.get)

          var source : scala.io.Source = null

          showEndlessProgress()

          if(subFile.isEmpty){
            source = scala.io.Source.fromFile(srcPath.get)
          } else {
            source = scala.io.Source.fromFile(srcPath.get + File.separator + subFile.get)
          }

          val files = ArrayBuffer[String]()

          for(line <- source.getLines()){
            files.append(line)
          }

          fixPermissions(files.toArray)
          pullFiles(files.toArray, destDir)
          mResult = Some(HashMap[String,String](destContext.get -> destDir))
          ready()
        } else if(fileToPull.isDefined){
          Log.debug("file to pull directly specified")
          pullFile(fileToPull.get, destDir)
          mResult = Some(HashMap[String,String](destContext.get -> destDir))
          ready()
        } else {
          setErrorMessage("error: no files to pull specified!")
        }
      } else {
        if(workingDir.isEmpty)setErrorMessage("error: working dir not defined!")
        if(destContext.isEmpty)setErrorMessage("error: destination context not defined")
      }
    } else {
      setErrorMessage("error: no parameters given!")
      ready()
    }
  }

  def pullFiles(filePaths:Array[String], destinationDir:String) : Unit = {
    for(path <- filePaths){
      pullFile(path, destinationDir)
    }
  }

  def pullFile(filePath:String, destinationDir:String) : Unit = {
    val file = new File(filePath)
    val fileName = file.getName

    Log.debug("path of file to pull: " + filePath)
    Log.debug("filename of file to pull: " + fileName)

    val executor = new SystemCommandExecutor()
    val command = Seq("adb", "-e", "pull", filePath, destinationDir + File.separator + fileName)
    Log.debug("command to execute: " + command)
    executor.executeSystemCommand(command)
    Log.debug("file pulled")
  }

  def fixPermissions(filePaths:Array[String]): Unit = {

    for(file <- filePaths){
      val fixPermissionCommand = new AdbCommand("chmod 777 " + file, permissionCallback)
      fixPermissionCommand.execute()
    }

    val waitCondition = Future {
      while(mFixedPermissionCount < filePaths.length){
        //do nothing
        Log.debug("count: " + mFixedPermissionCount)
        Log.debug("filepaths length: " + filePaths.length)
      }
      "ready"
    }

    Log.debug("now waiting for permissions to be fixed..")

    //wait for permissions to be fixed
    Await result(waitCondition, Duration.Inf)
  }

  def permissionCallback(output:String) : Unit = {
    mFixedPermissionCount += 1
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DescriptorSplitRegex)
  }

  /**
    * This will be called once you call the ready() method inside your plugin.
    * Please return ALL the changed values here as a map containing the key and the changed value.
    * If you did not change any values, simply return an empty map = Some(Map[String,String]())
    * If you want to delete values from the context, set the matching key to an empty string value.
    * Returning None here, will be interpreted as an error.
    *
    * @return a map containing all the changed values.
    */
  override def result: Option[Map[String, String]] = {
    mResult
  }

}
