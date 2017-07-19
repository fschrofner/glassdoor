package io.glassdoor.plugin.plugins.network.mitm

import java.io.File

import io.glassdoor.application._
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.sys.process.Process

/**
  * Created by Florian Schrofner on 1/24/17.
  */
class MitmProxy extends Plugin {
  var mResult : Option[Map[String, String]] = None

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)
    var givenPort : Option[String] = None

    val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)
    val destPath = workingDir.get + "/" + ContextConstant.Key.Mitm + "/network.log"

    if(parameterArray.isDefined){
      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            if(parameter.name == "stop" && MitmProxy.mitmProcess.isDefined){
              MitmProxy.mitmProcess.get.destroy()
              MitmProxy.mitmProcess = None

              //delete port from values
              val result = HashMap[String,String](
                ContextConstant.FullKey.DynamicAnalysisMitm -> "",
                ContextConstant.FullKey.ResultLogMitm -> destPath
              )
              mResult = Some(result)
              ready()
              return
            }

          case ParameterType.NamedParameter =>
            parameter.name match {
              case "port" | "p" =>
                givenPort = parameter.value
            }
          case ParameterType.Flag =>
        }
      }
    }

    if(MitmProxy.mitmProcess.isEmpty){
      createFolderStructure(destPath)

      var port = ""

      if(givenPort.isDefined){
        port = givenPort.get
      } else {
        port = "8989"
      }

      val command = ArrayBuffer[String]()
      command.append("mitmdump")
      command.append("-p " + port)
      command.append("-w " + destPath)

      val executor = new SystemCommandExecutor
      val process = executor.executeSystemCommandInBackground(command)

      MitmProxy.mitmProcess = process

      //TODO: find out if process successfully started
      val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisMitm -> port)
      mResult = Some(result)
    } else {
      Log.debug("error: mitm process already defined")
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

}

object MitmProxy {
  var mitmProcess : Option[Process] = None
}