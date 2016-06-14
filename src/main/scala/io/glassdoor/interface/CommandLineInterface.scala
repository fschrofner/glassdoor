package io.glassdoor.interface

import akka.actor.{ActorRef, Props}
import io.glassdoor.application.{Log, Context, CommandInterpreter, Command}
import io.glassdoor.bus.{Message, MessageEvent, EventBus}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance
import jline.console.ConsoleReader
import jline.console.completer.StringsCompleter

/**
  * Created by Florian Schrofner on 3/15/16.
  */
class CommandLineInterface extends UserInterface {

  var mConsole:Option[ConsoleReader] = None
  var mCompleter:Option[StringsCompleter] = None
  var mCommandLineReader:Option[ActorRef] = None


  override def receive: PartialFunction[Any, Unit] = {
    //let the default user interface trait handle all messages,
    //except commandline specific ones
    super.receive orElse {
      case CommandLineMessage(action, data) =>
        action match {
          case CommandLineInterfaceConstant.Action.handleLine =>
            if(data.isDefined){
              val line = data.get.asInstanceOf[String]
              handleLine(line)
            }
        }
    }
  }

  override def initialise(context: Context): Unit = {
    Log.debug("initialising interface..")

    val console = new ConsoleReader()
    console.clearScreen()
    console.setPrompt(">")
    mConsole = Some(console)

    startReadingFromCommandline()

//    var line:String = null
//    setupAutoComplete()

    //loop forever until exit command is called
    //TODO: write man + store default commands centrally
    //TODO: this loop blocks akka from receiving more messages

//    while({line = console.readLine();line} != "exit"){
//      handleLine(line)
//      println("continue with loop!")
//    }

    //terminate()
  }

  def startReadingFromCommandline():Unit = {
    //run reader as own thread in order to prevent blocking of interface changes
    val commandLineReader = context.system.actorOf(Props(new CommandLineReader(self)))
    mCommandLineReader = Some(commandLineReader)

    commandLineReader ! CommandLineMessage(CommandLineReaderConstant.Action.init, mConsole)
  }

  def handleLine(line:String):Unit = {
    Log.debug("handle line called!")
    //TODO: handle "exit"!
    val input = CommandInterpreter.interpret(line)

    if(input.isDefined){
      //don't show next command prompt, while there is still a task executing
      mConsole.get.resetPromptLine("","",0)

      //TODO: use list of system commands instead
      if(input.get.name == "install"){
        EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.installResource, Some(input.get.parameters))))
      } else {
        EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.applyPlugin, input)))
      }
    }

  }

//  def setupAutoComplete():Unit = {
//    //TODO: handover all possible commands (system commands + plugins + aliases)
//    val completer = new StringsCompleter()
//    mCompleter = Some(completer)
//  }

  override def showPluginList(plugins: Array[PluginInstance]): Unit = {
    if(mConsole.isDefined){
      val console = mConsole.get
      for(plugin:PluginInstance <- plugins){
        console.println(plugin.kind + ":" + plugin.name)
      }
    }
  }

  override def showProgress(taskId: Long, progress: Float): Unit = ???

  override def showEndlessProgress(taskId: Long): Unit = {
    if(mConsole.isDefined){
      val console = mConsole.get
      for(i <- 1 to 20){
        console.print("-")
      }
    }
  }

  override def taskCompleted(taskId: Long): Unit = {
    //TODO: only if no more tasks are executing
    //TODO: can't handle this
    Log.debug("interface received task completed")

    //wait for new commands
    if(mCommandLineReader.isDefined){
      val commandLineReader = mCommandLineReader.get
      commandLineReader ! CommandLineMessage(CommandLineReaderConstant.Action.read, None)
    }
  }
}

case class CommandLineMessage(action: String, data:Option[Any])

object CommandLineInterfaceConstant {
  object Action {
    val handleLine = "handleLine"
  }
}
