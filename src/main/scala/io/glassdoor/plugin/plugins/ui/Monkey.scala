package io.glassdoor.plugin.plugins.ui

import java.io.File

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.io.Source

/**
  * Created by Florian Schrofner on 2/24/17.
  */
class Monkey extends Plugin {
  private var mResult : Option[Map[String,String]] = None
  private var mPackageName: Option[String] = None

  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if (parameters != null && (parameters.length > 1 && parameters(0) == "start") || (parameters.length > 0 && parameters(0) == "stop")) {
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
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if (parameterArray.isDefined) {
      var command: Option[String] = None

      for (parameter <- parameterArray.get) {
        parameter.paramType match {
          case ParameterType.Parameter =>
            //first one being the application to trace
            if (command.isEmpty) {
              command = Some(parameter.name)
            } else if (mPackageName.isEmpty) {
              mPackageName = Some(parameter.name)
            }
        }
      }

      //read package name from context
      if (mPackageName.isEmpty) {
        val packageNamePath = data.get(ContextConstant.FullKey.ResultLogPackageName)
        if (packageNamePath.isDefined) {
          for (line <- Source.fromFile(packageNamePath.get + File.separator + "result.log").getLines()) {
            if (mPackageName.isEmpty) {
              mPackageName = Some(line)
            }
          }
        }
      }

      if (command.isDefined) {
        command.get match {
          case "start" =>
            startMonkeyTesting()
          case "stop" =>
            stopMonkeyTesting()
        }
      } else {
        setErrorMessage("error: command not defined")
      }

    }

    ready()
  }

  def startMonkeyTesting() : Unit = {
    if(!Monkey.monkeyTestingStarted){
      Monkey.monkeyTestingStarted = true
      startMonkeyTest()

      mResult = Some(Map[String, String](ContextConstant.FullKey.DynamicAnalysisUi -> mPackageName.get))
    } else {
      setErrorMessage("error: monkey testing already running!")
    }
  }

  def startMonkeyTest(): Unit = {
    Log.debug("monkey test started: " + Monkey.monkeyTestingStarted)
    if(mPackageName.isDefined && Monkey.monkeyTestingStarted){
      val command = new AdbCommand("monkey -p " + mPackageName.get + " --throttle 100 --pct-touch 80 --pct-motion 20 10", monkeyTestCallback)
      command.execute()
    }
  }

  def stopMonkeyTesting(): Unit = {
    if(Monkey.monkeyTestingStarted){
      Monkey.monkeyTestingStarted = false
      mResult = Some(Map[String, String](ContextConstant.FullKey.DynamicAnalysisUi -> ""))
    } else {
      setErrorMessage("error: no monkey tests running")
    }
  }

  def monkeyTestCallback(output: String): Unit = {
    if(Monkey.monkeyTestingStarted){
      startMonkeyTest()
    }
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

object Monkey {
  private var monkeyTestingStarted = false
}