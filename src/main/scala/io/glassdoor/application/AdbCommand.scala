package io.glassdoor.application

import java.io.{InputStream, OutputStream, PrintWriter}

import akka.event.Logging.Debug

import scala.io.Source
import scala.sys.process._

/**
  * Created by Florian Schrofner on 1/22/17.
  */
class AdbCommand(val command : String, val outputCallback : String => Unit){

  def execute(): Unit = {
    val adbConnection = new ProcessIO(adbInput, adbOutput, errorOutput)
    val adbCommand = Seq("adb", "-e", "shell")
    adbCommand.run(adbConnection)
  }

  private def adbOutput(in: InputStream) {
    Log.debug("adb output ready")
    val outputSource = Source.fromInputStream(in)
    val outputString = new StringBuilder()

    for(line <- outputSource.getLines()){
      Log.debug("line: " + line)
      outputString.append(line)
    }

    outputSource.close()
    outputCallback(outputString.toString())
  }

  private def adbInput(out: OutputStream) {
    Log.debug("adb input ready, executing: " + command)
    val writer = new PrintWriter(out)
    writer.println("su")
    writer.println(command)
    writer.close()
  }

  private def errorOutput(err: InputStream) {
    err.close()
  }
}
