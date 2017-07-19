package io.glassdoor.plugin.plugins.loader.apk

import io.glassdoor.application.{ContextConstant, SystemCommandExecutor}
import io.glassdoor.plugin.Plugin

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 1/29/17.
  */
class ApkInstaller extends Plugin {

  //TODO: save value
  var mResult : Option[Map[String,String]] = Some(Map[String,String]())

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    //TODO: get value from original-binary.apk and install it on the device
    val apk = data.get(ContextConstant.FullKey.OriginalBinaryApk)

    if(apk.isDefined){
      showEndlessProgress()
      val executor = new SystemCommandExecutor
      val command = ArrayBuffer[String]()
      command.append("adb")
      command.append("install")
      command.append(apk.get)
      executor.executeSystemCommand(command)
    } else {
      setErrorMessage("error: apk not defined")
    }

    ready()
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
