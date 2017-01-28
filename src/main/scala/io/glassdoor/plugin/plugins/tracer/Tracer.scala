package io.glassdoor.plugin.plugins.tracer

import java.io.File

import io.glassdoor.application._
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.collection.immutable.HashMap.HashMap1
import scala.collection.mutable.ArrayBuffer

/**
  * Created by flosch on 1/28/17.
  */
class Tracer extends Plugin {
  private var mResult : Option[Map[String,String]] = None
  private var mApplicationToTrace : Option[String] = None
  private var mWorkingDir : Option[String] = None

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    mWorkingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    //TODO: when tracer is stopped, copy file to pc and store in context
    //TODO: when starting to trace, save current application traced in context

    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){
      var command : Option[String] = None

      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            //first one being the application to trace
            if(command.isEmpty){
              command = Some(parameter.name)
            } else if(mApplicationToTrace.isEmpty){
              mApplicationToTrace = Some(parameter.name)
            }
        }
      }

      if(command.isDefined){
        command.get match {
          case "start" =>
            startTracer()
          case "stop" =>
            stopTracer()
        }
      } else {
        setErrorMessage("error: command not defined")
        ready()
      }

    }
  }

  def startTracer() : Unit = {
    if(mApplicationToTrace.isDefined){
      //we do not care about the output
      val command = new AdbCommand("gtrace " + mApplicationToTrace.get + " &", _ => Unit)
      command.execute()
      val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisTracer -> mApplicationToTrace.get)
      mResult = Some(result)
      ready()
    } else {
      setErrorMessage("error: application to trace not defined")
      ready()
    }
  }

  def stopTracer() : Unit = {
    //TODO: cancel tracer
    if(mWorkingDir.isDefined){
      val destPath = mWorkingDir.get + "/" + ContextConstant.Key.Tracer + "/result.log"
      createFolderStructure(destPath)

      val executor = new SystemCommandExecutor
      val commandBuffer = ArrayBuffer[String]()
      commandBuffer.append("adb")
      commandBuffer.append("pull")
      commandBuffer.append("/sdcard/gtrace.log")
      commandBuffer.append(destPath)
      executor.executeSystemCommand(commandBuffer)

      val result = HashMap[String,String](ContextConstant.FullKey.ResultLogTracer -> destPath)
      mResult = Some(result)
    } else {
      setErrorMessage("error: working directory not defined!")
    }
    ready()
  }

  def createFolderStructure(path:String):Unit = {
    val file = new File(path)
    file.getParentFile.mkdirs()
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

  override def help(parameters: Array[String]): Unit = ???
}
