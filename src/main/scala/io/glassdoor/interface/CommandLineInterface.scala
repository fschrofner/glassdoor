package io.glassdoor.interface

import java.io.PrintWriter
import java.util.concurrent.TimeUnit
import javax.smartcardio.TerminalFactory

import akka.actor.{ActorRef, Cancellable, Props}
import io.glassdoor.application.{Command, CommandInterpreter, Context, Log}
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.plugin.manager.PluginManagerConstant.PluginErrorCode
import io.glassdoor.plugin.resource.ResourceManagerConstant
import io.glassdoor.plugin.resource.ResourceManagerConstant.ResourceErrorCode
import io.glassdoor.resource.Resource
import jline.{Terminal, UnixTerminal}
import jline.console.ConsoleReader
import jline.console.completer.StringsCompleter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by Florian Schrofner on 3/15/16.
  */
class CommandLineInterface extends UserInterface {

  var mConsole:Option[ConsoleReader] = None
  var mCompleter:Option[StringsCompleter] = None
  var mCommandLineReader:Option[ActorRef] = None
  var mCounter = 0

  //TODO: there can be mutliple animations going on!
  var mAnimationTask:Option[Cancellable] = None


  override def receive: PartialFunction[Any, Unit] = {
    //let the default user interface trait handle all messages,
    //except commandline specific ones
    super.receive orElse {
      case CommandLineMessage(action, data) =>
        action match {
          case CommandLineInterfaceConstant.Action.HandleLine =>
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

    console.getTerminal.init()
    //console.clearScreen() //TODO: uncomment
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
    Log.debug("line: " + line)
    //TODO: handle "exit"!
    val input = CommandInterpreter.interpret(line)

    if(input.isDefined){
      //don't show next command prompt, while there is still a task executing
      mConsole.get.resetPromptLine("","",0)

      //TODO: use list of system commands instead
      //TODO: this should be moved out of the interface and be interpreted somewhere else
      if(input.get.name == "install") {
        Log.debug("install called!")
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.InstallResource, Some(input.get.parameters))))
      } else if(input.get.name == "remove"){
        Log.debug("remove called!")
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.RemoveResource, Some(input.get.parameters))))
      } else if(input.get.name == "update"){
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.UpdateAvailableResources,None)))
      } else if(input.get.name == "help"){
        if(input.get.parameters.length == 1){
          EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ShowPluginHelp, Some(input.get.parameters(0)))))
        } else {
          EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ShowPluginHelp, Some(""))))
        }
      } else if(input.get.name == "list"){
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ShowPluginList, None)))
      } else if(input.get.name == "exit"){
        Log.debug("exit called!")
        if(mConsole.isDefined){
          mConsole.get.shutdown()
          mConsole = None
        }
        terminate()
      } else {
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ApplyPlugin, input)))
      }
    }
  }



//  def setupAutoComplete():Unit = {
//    //TODO: handover all possible commands (system commands + plugins + aliases)
//    val completer = new StringsCompleter()
//    mCompleter = Some(completer)
//  }

  override def print(message: String): Unit = {
    Log.debug("commandline interface received print")
    if(mConsole.isDefined){
      Log.debug("console defined, printing..")
      Log.debug("message: " + message)
      mConsole.get.println(message)
      //TODO: can't print when waiting for line..
    } else {
      Log.debug("error: mConsole not defined")
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

  override def showProgress(taskInstance: PluginInstance, progress: Float): Unit = ???

  override def showEndlessProgress(taskInstance: PluginInstance): Unit = {
    Log.debug("commandline interface: showing endless progress")
    if(mConsole.isDefined){
      Log.debug("console defined")
      //val console = mConsole.get
      val handle = context.system.scheduler.schedule(Duration.Zero, Duration.create(1, TimeUnit.SECONDS))(updateEndlessProgress(taskInstance))
      mAnimationTask = Some(handle)
    } else {
      Log.debug("error: console not defined!")
    }
  }

  def updateEndlessProgress(taskInstance: PluginInstance):Unit = {
    if(mConsole.isDefined){
      val console = mConsole.get
      val infoString = "[" + taskInstance.uniqueId + "] " + taskInstance.name + ":"

      val stringBuilder = new StringBuilder()
      stringBuilder.append(CommandLineInterfaceConstant.Progress.StartString)

      for(i <- 1 to CommandLineInterfaceConstant.Progress.ProgressbarLength){
        if((i > mCounter && i <= mCounter + CommandLineInterfaceConstant.Progress.EndlessProgressLength)
          || (i < (mCounter + CommandLineInterfaceConstant.Progress.EndlessProgressLength) - CommandLineInterfaceConstant.Progress.ProgressbarLength)){
          stringBuilder.append(CommandLineInterfaceConstant.Progress.ProgressbarFilledString)
        } else {
          stringBuilder.append(CommandLineInterfaceConstant.Progress.ProgressbarEmptyString)
        }
      }

      stringBuilder.append(CommandLineInterfaceConstant.Progress.EndString)

      val terminalWidth = console.getTerminal.getWidth
      val spacing = terminalWidth - infoString.length - stringBuilder.length

      for(i <- 1 to spacing){
        stringBuilder.insert(0, " ")
      }

      //TODO: need to do something here
      //TODO: multithreading will not work here
      console.resetPromptLine("",infoString + stringBuilder.toString(),-1)

      mCounter += 1

      if(mCounter >= CommandLineInterfaceConstant.Progress.ProgressbarLength){
        mCounter = 0
      }
    }
  }

  override def taskCompleted(taskInstance: PluginInstance): Unit = {
    Log.debug("interface received task completed")

    if(mConsole.isDefined){
      stopAnimation(taskInstance)
      mCounter = 0

      //show completed task
      val infoString = "[" + taskInstance.uniqueId + "] " + taskInstance.name + ":"

      val stringBuilder = new StringBuilder()
      stringBuilder.append(CommandLineInterfaceConstant.Progress.StartString)

      for(i <- 1 to CommandLineInterfaceConstant.Progress.ProgressbarLength){
        stringBuilder.append(CommandLineInterfaceConstant.Progress.ProgressbarFilledString)
      }

      stringBuilder.append(CommandLineInterfaceConstant.Progress.EndString)

      val console = mConsole.get

      val terminalWidth = console.getTerminal.getWidth
      val spacing = terminalWidth - infoString.length - stringBuilder.length

      for(i <- 1 to spacing){
        stringBuilder.insert(0, " ")
      }

      console.resetPromptLine("",infoString + stringBuilder.toString(),-1)
      console.println()
    }
  }


  override def resourceCompleted(resource: Option[Resource], code: Int): Unit = {
    code match {
      case ResourceManagerConstant.ResourceSuccessCode.ResourceSuccessfullyInstalled =>
        if(resource.isDefined){
          Console.println("successfully installed resource: " + resource.get.name + "[" + resource.get.kind + "]")
        }
      case ResourceManagerConstant.ResourceSuccessCode.ResourceSuccessfullyRemoved =>
        if(resource.isDefined){
          Console.println("successfully removed resource: " + resource.get.name + "[" + resource.get.kind + "]")
        }
    }

    waitForInput()
  }

  def stopAnimation(taskInstance: PluginInstance):Unit = {
    //TODO: stop correct animation
    if(mAnimationTask.isDefined){
      mAnimationTask.get.cancel()
      mAnimationTask = None
    }
  }

  override def taskFailed(taskInstance: Option[PluginInstance], error: Int, data:Option[Any]): Unit = {
    if(taskInstance.isDefined){
      stopAnimation(taskInstance.get)
    }

    if(mConsole.isDefined){

      error match {
        case PluginErrorCode.DependenciesNotSatisfied =>
          if(data.isDefined){
            mConsole.get.println("error: dependency not satisfied: " + data.get.asInstanceOf[String])
          }
        case PluginErrorCode.DependenciesInChange =>
          if(data.isDefined){
            mConsole.get.println("error: dependency in change: " + data.get.asInstanceOf[String])
          }
        case PluginErrorCode.PluginNotFound =>
          mConsole.get.println("error: plugin not found!")
      }
    }

    waitForInput()
  }

  override def resourceFailed(resource: Option[Resource], error: Int, data: Option[Any]): Unit = {
    if(mConsole.isDefined){
      error match {
        case ResourceErrorCode.ResourceAlreadyInstalled =>
          if(resource.isDefined){
            mConsole.get.println("error: resource already installed: " + resource.get.name + "[" + resource.get.kind + "]")
          }
        case ResourceErrorCode.ResourceNotFound =>
          mConsole.get.print("error: resource not found!")
      }
    }

    //TODO: probably send waitForInput from resource manager
    waitForInput()
  }


  override def waitForInput(): Unit = {
    Log.debug("wait for input called!")
    if(mCommandLineReader.isDefined){
      val commandLineReader = mCommandLineReader.get
      commandLineReader ! CommandLineMessage(CommandLineReaderConstant.Action.read, None)
    }
  }

}

case class CommandLineMessage(action: String, data:Option[Any])

object CommandLineInterfaceConstant {
  object Action {
    val HandleLine = "handleLine"
  }
  object Progress{
    val StartString = "["
    val EndString = "]"
    val ProgressbarLength = 25
    val ProgressbarEmptyString = "-"
    val ProgressbarFilledString = "#"
    val EndlessProgressLength = 20
  }
}
