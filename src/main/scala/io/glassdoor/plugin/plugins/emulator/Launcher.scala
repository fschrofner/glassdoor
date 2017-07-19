package io.glassdoor.plugin.plugins.emulator

import java.io.File

import io.glassdoor.application.{AdbCommand, ContextConstant}
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.io.Source

/**
  * Created by Florian Schrofner on 2/5/17.
  */
class Launcher extends Plugin {

  var mResult : Option[Map[String, String]] = None


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if(parameters != null && parameters.length > 0){
      DynamicValues(uniqueId, Some(Array[String]()), None)
    } else {
      DynamicValues(uniqueId, Some(Array(ContextConstant.FullKey.ResultLogPackageName)), None)
    }
  }

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    //monkey -p PACKAGE-NAME -c android.intent.category.LAUNCHER 1
    var packageName : String = null
    if(parameters != null && parameters.length > 0){
      packageName = parameters(parameters.length - 1)
    } else {
      val packageNameOpt = data.get(ContextConstant.FullKey.ResultLogPackageName)
      if(packageNameOpt.isDefined){
        for (line <- Source.fromFile(packageNameOpt.get + File.separator + "result.log").getLines()) {
          if(packageName == null){
            packageName = line
          }
        }
      }
    }

    if(packageName != null){
      val command = new AdbCommand("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1", _ => Unit)
      command.execute()
      mResult = Some(Map[String,String]())
    } else {
      setErrorMessage("error: no package name specified/available in context")
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
