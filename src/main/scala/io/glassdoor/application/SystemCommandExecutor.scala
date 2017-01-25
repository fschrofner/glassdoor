package io.glassdoor.application

import scala.collection.mutable.ArrayBuffer
import scala.sys.process.ProcessLogger
import scala.sys.process._

/**
  * Created by Florian Schrofner on 7/2/16.
  */
class SystemCommandExecutor {

  private var mStdOut:Option[StringBuilder] = None
  private var mStdErr:Option[StringBuilder] = None
  private var mResultCode:Option[Int] = None

  def executeSystemCommand(command:Seq[String]): Option[String] = {
    try {
      var result:Option[String] = None

      val stdout = new StringBuilder
      val stderr = new StringBuilder

      mStdOut = Some(stdout)
      mStdErr = Some(stderr)

      val newLine= sys.props("line.separator")

      val processLogger = ProcessLogger(line => stdout.append(line + newLine),
        line => stderr.append(line + newLine))

      val resultCode = command ! processLogger

      mResultCode = Some(resultCode)

      if(resultCode == 0){
        result = Some(stdout.toString())
        result
      } else {
        result
      }
    } catch {
      case e: Exception =>
        None
    }
  }

  def executeSystemCommandInBackground(command:Seq[String]): Option[Process] = {
    try {
      var result: Option[String] = None

      val stdout = new StringBuilder
      val stderr = new StringBuilder

      mStdOut = Some(stdout)
      mStdErr = Some(stderr)

      val newLine = sys.props("line.separator")

      val shellCommand = ArrayBuffer[String]()
      shellCommand.append("sh")
      shellCommand.append("-c")
      shellCommand.append(command.mkString(" "))

      val processLogger = ProcessLogger(line => stdout.append(line + newLine),
        line => stderr.append(line + newLine))

      val process = shellCommand.run(processLogger)

      Some(process)
    } catch {
      case e: Exception =>
        None
    }
  }

  def getResultCode:Option[Int] = {
    mResultCode
  }

  def getResult:Option[String] = {
    if(mStdOut.isDefined){
      Some(mStdOut.get.toString())
    } else {
      None
    }
  }

  def lastCommandSuccessful:Boolean = {
    mResultCode.isDefined && mResultCode.get == 0
  }

  def getErrorOutput:Option[String] = {
    if(mStdErr.isDefined && !mStdErr.get.isEmpty){
      Some(mStdErr.get.toString())
    } else {
      None
    }
  }

}

