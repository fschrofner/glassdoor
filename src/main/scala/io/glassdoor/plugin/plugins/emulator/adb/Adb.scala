package io.glassdoor.plugin.plugins.emulator.adb

import io.glassdoor.application._
import io.glassdoor.plugin.Plugin

/**
  * Created by Florian Schrofner on 1/22/17.
  */
class Adb extends Plugin {
  var mResult : Option[Map[String,String]] = None
  var mCommandToExecute : Option[String] = None
  var mDestContext : Option[String] = None

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){

      for(parameter <- parameterArray.get) {
        parameter.paramType match {
          case ParameterType.Parameter =>
            //first is command, second is context
            if(mCommandToExecute.isEmpty){
              mCommandToExecute = Some(parameter.name)
            } else if(mDestContext.isEmpty){
              mDestContext = Some(parameter.name)
            }
        }
      }

    }

    if(mCommandToExecute.isDefined){
      val adbCommand = new AdbCommand(mCommandToExecute.get, x => commandExecuted(x))
      adbCommand.execute()
    } else {
      Log.debug("error: no command specified!")
      ready()
    }
  }

  def commandExecuted(output : String) : Unit = {
    if(mDestContext.isDefined){
      //TODO: write output to file and save in context
    }
    Log.debug("received adb command output: " + output)
    mResult = Some(Map[String,String]())
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
