package io.glassdoor.interface

import io.glassdoor.application.Context
import io.glassdoor.bus.{Message, MessageEvent, EventBus}
import io.glassdoor.controller.{ControllerPluginParameters, ControllerConstant}
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
    //TODO: interpret input
    val input = line.split(" ")

    //TODO: generate a list of available inputs to distinguish commands vs plugins

    if(input(0) == "list"){

    } else {
      //don't show next command prompt, while there is still a task executing
      mConsole.get.resetPromptLine("","",0)

      val inputBuffer = input.toBuffer
      inputBuffer.remove(0)
      val parameters = new ControllerPluginParameters(input(0), inputBuffer.toArray)
      EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.applyPlugin, Some(parameters))))
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

