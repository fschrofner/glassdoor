package io.glassdoor.interface

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import io.glassdoor.application.Log
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import org.jline.reader.LineReader
import org.jline.reader.impl.LineReaderImpl
import org.jline.reader.impl.completer.StringsCompleter

/**
  * Created by Florian Schrofner on 6/14/16.
  */
class CommandLineReader(mCommandLineInterface: ActorRef) extends Actor {

  var mConsole:Option[LineReader] = None

  override def receive: Receive = {
    case CommandLineMessage(action,data) =>
    action match {
      case CommandLineReaderConstant.Action.init =>
        if(data.isDefined){
          val console = data.get.asInstanceOf[LineReader]
          mConsole = Some(console)
          initialise()
        }
      case CommandLineReaderConstant.Action.read =>
        Log.debug("commandlinereader received read action")
        readLine()
    }
  }

  def initialise():Unit = {
    readLine()
  }

  def readLine(): Unit = {
    Log.debug("readline called in commandlinereader")

    if(mConsole.isDefined){
      val console = mConsole.get

      //make sure not to overwrite an existing line
      //console.drawLine()

      //TODO: sometimes the prompt line is not resetted correctly?
      //console.resetPromptLine("","",0)
      Log.debug("resetted prompt line, showing prompt")
      //console.setPrompt(">")

      Log.debug("waiting for input..")
      val line = console.readLine(">")
      mCommandLineInterface ! CommandLineMessage(CommandLineInterfaceConstant.Action.HandleLine, Some(line))
    }
  }
}

object CommandLineReaderConstant {
  object Action {
    val init = "init"
    val read = "read"
  }
}
