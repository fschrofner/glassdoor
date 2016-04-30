package io.glassdoor.interface

import io.glassdoor.application.{Context, CommandInterpreter, Command}
import io.glassdoor.bus.{Message, MessageEvent, EventBus}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance
import jline.console.ConsoleReader

/**
  * Created by Florian Schrofner on 3/15/16.
  */
class CommandLineInterface extends UserInterface {

  var mConsole:Option[ConsoleReader] = None

  override def initialise(context: Context): Unit = {
    println("initialising interface..")

    val console = new ConsoleReader()
    console.clearScreen()
    console.setPrompt(">")
    mConsole = Some(console)

    var line:String = null

    //loop forever until exit command is called
    //TODO: write man + store default commands centrally
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
      EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.applyPlugin, input)))
    }

  }

  override def showPluginList(plugins: Array[PluginInstance]): Unit = {
    if(mConsole.isDefined){
      val console = mConsole.get
      for(plugin:PluginInstance <- plugins){
        console.println(plugin.kind + ":" + plugin.name)
      }
    }
  }

  override def showProgress(taskName: String, progress: Float): Unit = ???

  override def showEndlessSpinner(taskName: String, show: Boolean): Unit = {
    if(!show && mConsole.isDefined){
      mConsole.get.setPrompt(">")
    }
  }
}

