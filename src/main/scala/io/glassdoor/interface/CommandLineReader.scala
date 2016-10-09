package io.glassdoor.interface

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import io.glassdoor.application.Log
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import jline.console.ConsoleReader
import jline.console.completer.StringsCompleter

/**
  * Created by Florian Schrofner on 6/14/16.
  */
class CommandLineReader(mCommandLineInterface: ActorRef) extends Actor {

  var mConsole:Option[ConsoleReader] = None
  var mCompleter:Option[StringsCompleter] = None

  override def receive: Receive = {
    case CommandLineMessage(action,data) =>
    action match {
      case CommandLineReaderConstant.Action.init =>
        if(data.isDefined){
          val console = data.get.asInstanceOf[ConsoleReader]
          mConsole = Some(console)
          initialise()
        }
      case CommandLineReaderConstant.Action.read =>
        readLine()
    }
  }

  def initialise():Unit = {
    setupAutoComplete()
    readLine()
  }

  def setupAutoComplete():Unit = {
    //TODO: handover all possible commands (system commands + plugins + aliases)
    val completer = new StringsCompleter()
    mCompleter = Some(completer)
  }

  def readLine(): Unit = {
    Log.debug("readline called in commandlinereader")

    if(mConsole.isDefined){
      val console = mConsole.get

      //make sure not to overwrite an existing line
      console.drawLine()
      console.resetPromptLine("","",-1)
      console.setPrompt(">")

      val line = console.readLine()
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
