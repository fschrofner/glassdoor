package io.glassdoor.plugin.plugins.emulator

import io.glassdoor.application.{AdbCommand, ContextConstant, Log, SystemCommandExecutor}
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.File

/**
  * Created by Florian Schrofner on 1/18/17.
  */
class Emulator extends Plugin {

  var mResult:Option[Map[String, String]] = None
  val mExecutor = new SystemCommandExecutor

  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    val emulatorPath = data.get(ContextConstant.FullKey.ConfigEmulatorRepositoryPath)

    if(emulatorPath.isDefined && emulatorPath.get != "PLACEHOLDER"){
      Log.debug("emulator path defined")
      var systemImagePath = ""

      //use system image defined in parameters
      if (parameters.length > 0){
        systemImagePath = parameters(0)
      } else {
        systemImagePath = emulatorPath.get + File.separator + "system.img"
      }

      val scriptPath = emulatorPath.get + File.separator + "scripts" + File.separator + "run_emulator.sh"

      val resultCode = startEmulator(scriptPath, systemImagePath)

      if(resultCode.isDefined && resultCode.get == 0){
        val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisEmulator -> "true")
        mResult = Some(result)
      } else {
        //there was an error
        errorMessage = mExecutor.getErrorOutput
      }
      Log.debug("emulator ready")
    } else {
      Log.debug("emulator path undefined")
      errorMessage = Some("error: emulator repository path not defined inside config!")
    }

    ready()
  }

  def startEmulator(scriptPath : String, systemImagePath : String) : Option[Int] = {
    showEndlessProgress()
    Log.debug("starting emulator with " + scriptPath + " using " + systemImagePath)
    val command = ArrayBuffer[String]()
    command.append(scriptPath)
    command.append(systemImagePath)
    mExecutor.executeSystemCommand(command)
    Log.debug("emulator started")
    mExecutor.getResultCode
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
