package io.glassdoor.interface

import java.io.{PrintWriter, Writer}
import collection.JavaConverters._
import java.util
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
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color
import org.jline.reader.impl.LineReaderImpl
import org.jline.reader.{Completer, Highlighter, LineReader, LineReaderBuilder}
import org.jline.reader.impl.completer.{AggregateCompleter, FileNameCompleter, StringsCompleter}
import org.jline.terminal.TerminalBuilder
import org.jline.utils.{AttributedString, AttributedStringBuilder}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by Florian Schrofner on 3/15/16.
  */
class CommandLineInterface extends UserInterface {

  //mConsoleOutput should be used for simple outputs, that will not change
  //while mConsole should be used for lines that will be updated
  var mConsole:Option[LineReader] = None
  var mConsoleOutput:Option[PrintWriter] = None

  var mCompleter:Option[Completer] = None
  var mPluginCommandList:Option[Array[String]] = None
  var mAliasCommandList:Option[Array[String]] = None
  var mContextKeys:Option[Array[String]] = None
  val mSystemCommands:Array[String] = Array[String]("help", "list", "exit", "update", "add", "remove")

  var mCommandLineReader:Option[ActorRef] = None
  var mPluginsShowingProgress:Array[PluginProgress] = Array[PluginProgress]()

  //TODO: there can be mutliple animations going on!
  var mAnimationTask:Option[Cancellable] = None

  val newLine = sys.props("line.separator")

  var nrOfPrintedEndlessProgresses = 0


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

    val terminal = TerminalBuilder.builder().system(true).build()
    val console = LineReaderBuilder.builder().terminal(terminal).build()

    AnsiConsole.systemInstall()

    console.getTerminal.flush()
    mConsoleOutput = Some(terminal.writer())

    mConsole = Some(console)

    startReadingFromCommandline()
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
      //mConsole.get.resetPromptLine("","",0)

      //TODO: use list of system commands instead
      //TODO: this should be moved out of the interface and be interpreted somewhere else
      if(input.get.name == "add") {
        Log.debug("add called!")
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
          Log.debug("console defined")
          //mConsole.get.
          //mConsole.get.shutdown()
          Log.debug("shut down")
          mConsole = None
        }
        terminate()
        System.exit(1)
      } else {
        EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ApplyPlugin, input)))
      }
    } else {
      Log.debug("input not defined!")
      waitForInput()
    }
  }

  override def print(message: String): Unit = {
    stopProgressUpdates()
    clearEndlessProgresses()

    Log.debug("commandline interface received print")
    if(mConsoleOutput.isDefined){
      Log.debug("console defined, printing..")
      Log.debug("message: " + message)
      mConsoleOutput.get.println(message)
    } else {
      Log.debug("error: mConsole not defined")
    }

    startProgressUpdates()
  }

  override def showPluginList(plugins: Array[PluginInstance]): Unit = {
    if(mConsoleOutput.isDefined){
      val console = mConsoleOutput.get
      for(plugin:PluginInstance <- plugins){
        console.append(plugin.kind + ":" + plugin.name + newLine)
      }
      console.flush()
    }
  }

  override def showProgress(taskInstance: PluginInstance, progress: Float): Unit = ???

  override def showEndlessProgress(taskInstance: PluginInstance): Unit = {
    Log.debug("commandline interface: showing endless progress")
    mPluginsShowingProgress = mPluginsShowingProgress :+ PluginProgress(taskInstance, 0, true)

    if(mConsole.isDefined){
      Log.debug("console defined")

      //restart progress updates
      stopProgressUpdates()
      startProgressUpdates()
    } else {
      Log.debug("error: console not defined!")
    }
  }

  def startProgressUpdates(): Unit ={
    if(mPluginsShowingProgress.length > 0){
      val handle = context.system.scheduler.schedule(Duration.Zero, Duration.create(1, TimeUnit.SECONDS))(updateProgresses())
      mAnimationTask = Some(handle)
    }
  }

  def stopProgressUpdates(): Unit ={
    this.synchronized{
      if(mAnimationTask.isDefined){
        mAnimationTask.get.cancel()
        mAnimationTask = None
      }
    }
  }

  def clearEndlessProgresses() : Unit = {
    if(nrOfPrintedEndlessProgresses > 0){

      val ansiUp = Ansi.ansi()
      val ansiErase = Ansi.ansi()

      ansiUp.cursorUpLine()
      ansiErase.eraseLine(Ansi.Erase.ALL)
      ansiErase.cursorToColumn(0)


      for(i <- 0 until nrOfPrintedEndlessProgresses){
        mConsoleOutput.get.print(ansiUp.toString)
        mConsoleOutput.get.flush()

        mConsoleOutput.get.print(ansiErase.toString)
        mConsoleOutput.get.flush()
      }

      nrOfPrintedEndlessProgresses = 0
    }
  }

  def updateProgresses() : Unit = {
    this.synchronized {
      clearEndlessProgresses()

      val stringBuilder = new StringBuilder

      for(pluginProgress <- mPluginsShowingProgress){
        if(pluginProgress.endlessProgress){
          val result = updateEndlessProgress(pluginProgress.pluginInstance, pluginProgress.progress)
          pluginProgress.progress = result.progressValue
          stringBuilder.append(result.progressString + newLine)
          nrOfPrintedEndlessProgresses += 1
        } else {
          //TODO
        }
      }

      if(mConsoleOutput.isDefined){
        Log.debug("printing endless progresses. nr of progresses: " + nrOfPrintedEndlessProgresses)
        mConsoleOutput.get.print(stringBuilder.toString())
        mConsoleOutput.get.flush()
      }
    }
  }

  def updateEndlessProgress(taskInstance: PluginInstance, counter:Int):UpdateProgressResult = {
    if(mConsole.isDefined){
      val console = mConsole.get
      val infoString = "[" + taskInstance.uniqueId + "] " + taskInstance.name + ":"

      val stringBuilder = new StringBuilder()
      stringBuilder.append(CommandLineInterfaceConstant.Progress.StartString)

      for(i <- 1 to CommandLineInterfaceConstant.Progress.ProgressbarLength){
        if((i > counter && i <= counter + CommandLineInterfaceConstant.Progress.EndlessProgressLength)
          || (i < (counter + CommandLineInterfaceConstant.Progress.EndlessProgressLength) - CommandLineInterfaceConstant.Progress.ProgressbarLength)){
          stringBuilder.append(CommandLineInterfaceConstant.Progress.ProgressbarFilledString)
        } else {
          stringBuilder.append(CommandLineInterfaceConstant.Progress.ProgressbarEmptyString)
        }
      }

      stringBuilder.append(CommandLineInterfaceConstant.Progress.EndString)

      val terminalWidth = console.getTerminal.getWidth
      val spacing = terminalWidth - infoString.length - stringBuilder.length

      stringBuilder.insert(0, " " * spacing)

      val resultString = infoString + stringBuilder.toString()
      var resultInt = counter + 1

      if(resultInt >= CommandLineInterfaceConstant.Progress.ProgressbarLength){
        resultInt = 0
      }

      UpdateProgressResult(resultString, resultInt)
    } else {
      UpdateProgressResult("", 0)
    }
  }

  override def taskCompleted(taskInstance: PluginInstance): Unit = {
    this.synchronized{
      Log.debug("interface received task completed")
      Log.debug(taskInstance.name + " COMPLETED")

      if(mConsole.isDefined && mConsoleOutput.isDefined){

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

        stopAnimation(taskInstance)

        print(infoString + stringBuilder.toString())
      }
    }
  }


  override def resourceCompleted(resource: Option[Resource], code: Int): Unit = {
    code match {
      case ResourceManagerConstant.ResourceSuccessCode.ResourceSuccessfullyInstalled =>
        if(resource.isDefined){
          print("successfully installed resource: " + resource.get.name + "[" + resource.get.kind + "]")
        }
      case ResourceManagerConstant.ResourceSuccessCode.ResourceSuccessfullyRemoved =>
        if(resource.isDefined){
          print("successfully removed resource: " + resource.get.name + "[" + resource.get.kind + "]")
        }
    }

    waitForInput()
  }

  def stopAnimation(taskInstance: PluginInstance):Unit = {
    mPluginsShowingProgress = mPluginsShowingProgress.filterNot(_.pluginInstance.uniqueId == taskInstance.uniqueId)
    stopProgressUpdates()
  }

  override def taskFailed(taskInstance: Option[PluginInstance], error: Int, data:Option[Any]): Unit = {
    if(taskInstance.isDefined){
      val pluginProgress = mPluginsShowingProgress.find(_.pluginInstance.uniqueId == taskInstance.get.uniqueId)
      if(pluginProgress.isDefined){
        stopAnimation(taskInstance.get)
      }
    }

    if(mConsoleOutput.isDefined){
      error match {
        case PluginErrorCode.DependenciesNotSatisfied =>
          if(data.isDefined){
            print("error: dependency not satisfied: " + data.get.asInstanceOf[String])
          }
        case PluginErrorCode.DependenciesInChange =>
          if(data.isDefined){
            print("error: dependency in change: " + data.get.asInstanceOf[String])
          }
        case PluginErrorCode.PluginNotFound =>
          print("error: plugin not found!")
      }
    }
  }

  override def resourceFailed(resource: Option[Resource], error: Int, data: Option[Any]): Unit = {
    if(mConsoleOutput.isDefined){
      error match {
        case ResourceErrorCode.ResourceAlreadyInstalled =>
          if(resource.isDefined){
            print("error: resource already installed: " + resource.get.name + "[" + resource.get.kind + "]")
          }
        case ResourceErrorCode.ResourceNotFound =>
          print("error: resource not found!")
      }
    }

    //TODO: probably send waitForInput from resource manager
    waitForInput()
  }


  override def waitForInput(): Unit = {
    Log.debug("wait for input called!")

    //cancel animations that might be still going on
    stopProgressUpdates()
    mPluginsShowingProgress = Array()

    if(mCommandLineReader.isDefined){
      Log.debug("command line reader defined")
      val commandLineReader = mCommandLineReader.get
      commandLineReader ! CommandLineMessage(CommandLineReaderConstant.Action.read, None)
    } else {
      Log.debug("command line reader not defined")
    }
  }

  def reloadCompletions(): Unit ={
    val stringsBuffer = ArrayBuffer[String]()

    stringsBuffer.appendAll(mSystemCommands)

    if(mPluginCommandList.isDefined){
      stringsBuffer.appendAll(mPluginCommandList.get)
    }

    if(mAliasCommandList.isDefined){
      stringsBuffer.appendAll(mAliasCommandList.get)
    }

    if(mContextKeys.isDefined){
      stringsBuffer.appendAll(mContextKeys.get)
    }

    //TODO: also add other commands like exit, list & help

    val stringsCompleter = new StringsCompleter(stringsBuffer.toArray:_*)
    val fileNamesCompleter = new FileNameCompleter()
    val completer = new AggregateCompleter(ArrayBuffer(stringsCompleter, fileNamesCompleter).asJava)

    mCompleter = Some(completer)

    if(mConsole.isDefined){
      mConsole.get.asInstanceOf[LineReaderImpl].setCompleter(mCompleter.get)
    }
  }

  def reloadHighlighter():Unit = {
    val stringsBuffer = ArrayBuffer[String]()

    stringsBuffer.appendAll(mSystemCommands)

    if(mPluginCommandList.isDefined){
      stringsBuffer.appendAll(mPluginCommandList.get)
    }

    if(mAliasCommandList.isDefined){
      stringsBuffer.appendAll(mAliasCommandList.get)
    }

    val highlighter = new CommandLineHighlighter(Some(stringsBuffer.toArray), mContextKeys)

    if(mConsole.isDefined){
      mConsole.get.asInstanceOf[LineReaderImpl].setHighlighter(highlighter)
    }
  }

  override def handlePluginCommandList(commands: Array[String]): Unit = {
    mPluginCommandList = Some(commands)
    reloadCompletions()
    reloadHighlighter()
  }

  override def handleAliasList(aliases: Array[String]): Unit = {
    mAliasCommandList = Some(aliases)
    reloadCompletions()
    reloadHighlighter()
  }

  override def handleContextList(context: Array[String]): Unit = {
    mContextKeys = Some(context)
    reloadCompletions()
    reloadHighlighter()
  }
}

case class CommandLineMessage(action: String, data:Option[Any])
case class PluginProgress(pluginInstance: PluginInstance, var progress:Int, endlessProgress:Boolean)
case class UpdateProgressResult(progressString:String, progressValue:Int)

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
