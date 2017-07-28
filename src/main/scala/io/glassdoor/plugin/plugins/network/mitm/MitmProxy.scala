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
  val MITM_LOG_FILE = "network.dump"
  val SSL_KEY_CONFIG = "sslkeyconfig.log"
  val TCP_DUMP = "network.cap"

  var mPluginPath = ""
  var mEmulatorRepositoryPath = ""

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
    mEmulatorRepositoryPath = data.get(ContextConstant.FullKey.ConfigEmulatorRepositoryPath).get
    mPluginPath =  workingDir.get + File.separator + ContextConstant.Key.Mitm

    if(parameterArray.isDefined){
      for(parameter <- parameterArray.get){
        parameter.paramType match {
          case ParameterType.Parameter =>
            if(parameter.name == "stop" && MitmProxy.mitmProcess.isDefined){
              stopSniffing()
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

    startSniffing(givenPort)
  }

  def startSniffing(givenPort:Option[String]):Unit = {
    if(MitmProxy.mitmProcess.isEmpty){

      val mitmLogPath = mPluginPath + File.separator + MITM_LOG_FILE
      createFolderStructure(mitmLogPath)

      var port = ""

      if(givenPort.isDefined){
        port = givenPort.get
      } else {
        port = "8989"
      }

      val certPath = mEmulatorRepositoryPath + File.separator + "certificates" + File.separator + "ca.pem"

      val command = ArrayBuffer[String]()

      //setting the environment variable is required to capture TLS master secrets
      command.append("env")
      command.append("MITMPROXY_SSLKEYLOGFILE=" + mPluginPath + File.separator + SSL_KEY_CONFIG)

      command.append("mitmdump")
      command.append("-p " + port)
      command.append("-w " + mitmLogPath)
      command.append("--cert " + certPath)
      command.append("--no-http2")

      val executor = new SystemCommandExecutor
      val process = executor.executeSystemCommandInBackground(command)

      MitmProxy.mitmProcess = process

      //tcpdump allows to capture traffic on the device itself
      val tcpCommand = new AdbCommand("tcpdump -w /sdcard/" + TCP_DUMP, _ => Unit)
      tcpCommand.execute()

      //TODO: find out if process successfully started
      val result = HashMap[String,String](ContextConstant.FullKey.DynamicAnalysisMitm -> port)
      mResult = Some(result)
    } else {
      Log.debug("error: mitm process already defined")
    }

    ready()
  }

  def stopSniffing():Unit = {
    //stop tcp dump, return result in callback
    val killTcpCommand = new AdbCommand("killall -SIGINT tcpdump", killTcpDumpCallback)
    killTcpCommand.execute()
  }

  def killTcpDumpCallback(output:String):Unit = {
    //create log paths to save
    val mitmLogPath = mPluginPath + File.separator + MITM_LOG_FILE
    val tcpLogPath = mPluginPath + File.separator + TCP_DUMP
    val sslKeyPath = mPluginPath + File.separator + SSL_KEY_CONFIG

    //stop mitm proxy
    MitmProxy.mitmProcess.get.destroy()
    MitmProxy.mitmProcess = None

    //pull tcp dump
    val executor = new SystemCommandExecutor
    val commandBuffer = ArrayBuffer[String]()
    commandBuffer.append("adb")
    commandBuffer.append("pull")
    commandBuffer.append("/sdcard/" + TCP_DUMP)
    commandBuffer.append(tcpLogPath)

    executor.executeSystemCommand(commandBuffer)

    //remove dump from emulator
    new AdbCommand("rm /sdcard/" + TCP_DUMP, _ => Unit).execute()

    //delete port from values and save results
    val result = HashMap[String,String](
      ContextConstant.FullKey.DynamicAnalysisMitm -> "",
      ContextConstant.FullKey.ResultLogMitmDump -> mitmLogPath,
      ContextConstant.FullKey.ResultLogMitmTcpDump -> tcpLogPath,
      ContextConstant.FullKey.ResultLogMitmSslKeyConfig -> sslKeyPath
    )

    mResult = Some(result)
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