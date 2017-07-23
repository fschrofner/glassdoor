package io.glassdoor.plugin.plugins.fs

import java.io.File

import io.glassdoor.application._
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 3/4/17.
  */
class FsChanges extends Plugin {

  private var mWorkingDir : Option[String] = None
  private var mResult : Option[Map[String,String]] = None

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    mWorkingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)
    val paramsArray = CommandInterpreter.parseToParameterArray(parameters)

    var command : Option[String] = None

    if(paramsArray.isDefined){
      for(parameter <- paramsArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            if(command.isEmpty){
              command = Some(parameter.name)
            }
        }
      }

      if(command.isDefined){
        command.get.toLowerCase match {
          case "start" =>
            startMonitoring()
          case "stop" =>
            stopMonitoring()
        }
      }
    } else {
      ready()
    }
  }

  def startMonitoring() : Unit = {
    val command = new AdbCommand("fsm-start", startCallback)
    command.execute()
  }

  def stopMonitoring() : Unit = {
    val command  = new AdbCommand("fsm-stop", stopCallback)
    command.execute()
  }

  def startCallback(output: String) : Unit = {
    val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisFs -> "true")
    mResult = Some(result)
    ready()
  }

  def stopCallback(output: String) : Unit = {
    if(mWorkingDir.isDefined){
      val destPath = mWorkingDir.get + File.separator + ContextConstant.Key.Fs + File.separator + "result.log"
      createFolderStructure(destPath)

      val executor = new SystemCommandExecutor
      val commandBuffer = ArrayBuffer[String]()
      commandBuffer.append("adb")
      commandBuffer.append("pull")
      commandBuffer.append("/sdcard/filesystem.log")
      commandBuffer.append(destPath)
      executor.executeSystemCommand(commandBuffer)

      val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisFs -> "", ContextConstant.FullKey.ResultLogFs -> destPath)
      mResult = Some(result)

      removeFilesFromAndroid()
    } else {
      setErrorMessage("error: working directory not defined!")
    }

    ready()
  }

  def removeFilesFromAndroid() : Unit = {
    new AdbCommand("rm /sdcard/timestamp", _ => Unit).execute()
    new AdbCommand("rm /sdcard/filesystem.log", _ => Unit).execute()
  }

  def createFolderStructure(path:String):Unit = {
    val file = new File(path)
    file.getParentFile.mkdirs()
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
