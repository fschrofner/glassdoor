package io.glassdoor.interface

import io.glassdoor.application.{Context, CommandInterpreter, Command}
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

  override def initialise(context: Context): Unit = {
    println("initialising interface..")

    val console = new ConsoleReader()
    console.clearScreen()
    console.setPrompt(">")
    mConsole = Some(console)

    var line:String = null
    setupAutoComplete()

    //loop forever until exit command is called
    //TODO: write man + store default commands centrally
    //TODO: this loop blocks akka from receiving more messages

    //probably keep sending messages to self
    while({line = console.readLine();line} != "exit"){
      console.println("line: " + line)
      handleLine(line)
    }

    terminate()
  }

  def handleLine(line:String):Unit = {
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

  def setupAutoComplete():Unit = {
    //TODO: handover all possible commands (system commands + plugins + aliases)
    val completer = new StringsCompleter()
    mCompleter = Some(completer)
  }

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

  }

  override def taskCompleted(taskId: Long): Unit = {
    //TODO: only if no more tasks are executing
    //TODO: can't handle this
    println("interface received task completed")
    if(mConsole.isDefined){
      mConsole.get.setPrompt(">")
    }
  }
}

