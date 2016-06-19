package io.glassdoor.interface

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import io.glassdoor.bus.{Message, MessageEvent, EventBus}
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
    if(mConsole.isDefined){
      val console = mConsole.get
      mConsole.get.resetPromptLine("","",-1)
      mConsole.get.setPrompt(">")

      val line = console.readLine()
      //while({line = console.readLine();line} != "exit"){
        //send line to commandline interface
      mCommandLineInterface ! CommandLineMessage(CommandLineInterfaceConstant.Action.HandleLine, Some(line))
      //}

      //terminate program
      //EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.terminate, None)))
    }
  }
}

object CommandLineReaderConstant {
  object Action {
    val init = "init"
    val read = "read"
  }
}
