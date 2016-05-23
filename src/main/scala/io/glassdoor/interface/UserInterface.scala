package io.glassdoor.interface

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.Context
import io.glassdoor.bus.{MessageEvent, EventBus, Message}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginInstance

/**
  * Created by Florian Schrofner on 3/15/16.
  */
trait UserInterface extends Actor {
  def initialise(context:Context):Unit
  def showProgress(taskId:Long, progress: Float):Unit
  def showEndlessProgress(taskId:Long):Unit
  def taskCompleted(taskId: Long):Unit

  def terminate():Unit = {
    EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.terminate, None)))
  }


  def showPluginList(plugins:Array[PluginInstance]):Unit

  def receive = {
    case Message(action, data) =>
      action match {
        case UserInterfaceConstant.Action.initialise =>
          if(data.isDefined){
            initialise(data.get.asInstanceOf[Context])
          }
        case UserInterfaceConstant.Action.showPluginList =>
          if(data.isDefined){
            showPluginList(data.get.asInstanceOf[Array[PluginInstance]])
          }
        case UserInterfaceConstant.Action.showEndlessProgress =>
          if(data.isDefined){
            val taskId = data.get.asInstanceOf[Long]
            showEndlessProgress(taskId)
          }
        case UserInterfaceConstant.Action.taskCompleted =>
          println("received task completed in user interface")
          if(data.isDefined){
            val taskId = data.get.asInstanceOf[Long]
            taskCompleted(taskId)
          }
    }
  }
}


object UserInterfaceConstant {
  val channel = "/interface"
  object Action {
    val initialise = "initialise"
    val showPluginList = "showPluginList"
    val showEndlessProgress = "showEndlessProgress"
    val showProgress = "showProgress"
    val taskCompleted = "taskCompleted"
  }
}
