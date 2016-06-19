package io.glassdoor.interface

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Log, Context}
import io.glassdoor.bus.{MessageEvent, EventBus, Message}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.plugin.manager.PluginErrorMessage

/**
  * Created by Florian Schrofner on 3/15/16.
  */
trait UserInterface extends Actor {
  def initialise(context:Context):Unit
  def showProgress(taskId:Long, progress: Float):Unit
  def showEndlessProgress(taskId:Long):Unit
  def taskCompleted(taskId: Long):Unit
  def taskFailed(taskId: Long, error: Int, data:Option[Any]):Unit

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
            val taskId = data.get.asInstanceOf[Long]
            showEndlessProgress(taskId)
          }
        case UserInterfaceConstant.Action.TaskCompleted =>
          Log.debug("received task completed in user interface")
          if(data.isDefined){
            val taskId = data.get.asInstanceOf[Long]
            taskCompleted(taskId)
          }
        case UserInterfaceConstant.Action.PluginError =>
          Log.debug("received task error in user interface")
          if(data.isDefined){
            val message = data.get.asInstanceOf[PluginErrorMessage]
            taskFailed(message.pluginId, message.errorCode, message.data)
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
  }
}
