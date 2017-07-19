package io.glassdoor.interface

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Context, Log}
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.plugin.manager.PluginErrorMessage
import io.glassdoor.plugin.resource.{ResourceErrorMessage, ResourceSuccessMessage}
import io.glassdoor.resource.Resource

/**
  * Created by Florian Schrofner on 3/15/16.
  */
trait UserInterface extends Actor {
  def initialise(context:Context):Unit
  def showProgress(taskInstace: PluginInstance, progress: Float):Unit
  def showEndlessProgress(task: PluginInstance):Unit
  def taskCompleted(taskInstance: PluginInstance):Unit
  def taskFailed(taskInstance: Option[PluginInstance], error: Int, data:Option[Any]):Unit
  def resourceCompleted(resource:Option[Resource], code:Int)
  def resourceFailed(resource:Option[Resource], error:Int, data:Option[Any]):Unit
  def waitForInput():Unit
  def print(message:String):Unit
  def handlePluginCommandList(commands:Array[String]):Unit
  def handleAliasList(aliases:Array[String]):Unit
  def handleContextList(context:Array[String]):Unit

  def terminate():Unit = {
    EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.Terminate, None)))
  }
  def showPluginList(plugins:Array[PluginInstance]):Unit

  override def receive = {
    case Message(action, data) =>
      action match {
        case UserInterfaceConstant.Action.Initialise =>
          if(data.isDefined){
            initialise(data.get.asInstanceOf[Context])
          }
        case UserInterfaceConstant.Action.ShowPluginList =>
          if(data.isDefined){
            showPluginList(data.get.asInstanceOf[Array[PluginInstance]])
          }
        case UserInterfaceConstant.Action.ShowEndlessProgress =>
          if(data.isDefined){
            Log.debug("received show endless progress")
            val taskInstance = data.get.asInstanceOf[PluginInstance]
            showEndlessProgress(taskInstance)
          }
        case UserInterfaceConstant.Action.TaskCompleted =>
          Log.debug("received task completed in user interface")
          if(data.isDefined){
            val taskInstance = data.get.asInstanceOf[PluginInstance]
            taskCompleted(taskInstance)
          }
        case UserInterfaceConstant.Action.ResourceSuccess =>
          Log.debug("received resource completed in user itnerface")
          if(data.isDefined){
            val message = data.get.asInstanceOf[ResourceSuccessMessage]
            resourceCompleted(message.resource, message.code)
          }
        case UserInterfaceConstant.Action.PluginError =>
          Log.debug("received task error in user interface")
          if(data.isDefined){
            val message = data.get.asInstanceOf[PluginErrorMessage]
            taskFailed(message.pluginInstance, message.errorCode, message.data)
          }
        case UserInterfaceConstant.Action.ResourceError =>
          Log.debug("received resource error in user interface")
          if(data.isDefined){
            val message = data.get.asInstanceOf[ResourceErrorMessage]
            resourceFailed(message.resource, message.errorCode, message.data)
          }
        case UserInterfaceConstant.Action.WaitForInput =>
          waitForInput()
        case UserInterfaceConstant.Action.Print =>
          if(data.isDefined){
            val message = data.get.asInstanceOf[String]
            print(message)
          } else {
            Log.debug("error: no data specified to print!")
          }
        case UserInterfaceConstant.Action.PluginCommandList =>
          if(data.isDefined){
            val commands = data.get.asInstanceOf[Array[String]]
            handlePluginCommandList(commands)
          }
        case UserInterfaceConstant.Action.AliasList =>
          if(data.isDefined){
            val aliases = data.get.asInstanceOf[Array[String]]
            handleAliasList(aliases)
          }
        case UserInterfaceConstant.Action.ContextList =>
          if(data.isDefined){
            val contextList = data.get.asInstanceOf[Array[String]]
            handleContextList(contextList)
          }
    }
  }
}


object UserInterfaceConstant {
  val Channel = "/interface"
  object Action {
    val Initialise = "initialise"
    val ShowPluginList = "showPluginList"
    val ShowEndlessProgress = "showEndlessProgress"
    val ShowProgress = "showProgress"
    val TaskCompleted = "taskCompleted"
    val PluginError = "pluginError"
    val PluginCommandList = "pluginCommandList"
    val AliasList = "aliasList"
    val ContextList = "contextList"
    val ResourceSuccess = "resourceCompleted"
    val ResourceError = "resourceError"
    val WaitForInput = "waitForInput"
    val Print = "print"
  }
}
