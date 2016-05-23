package io.glassdoor.plugin

import akka.actor.Actor
import io.glassdoor.application.Context
import io.glassdoor.bus.{MessageEvent, EventBus, Message}
import io.glassdoor.interface.{UserInterfaceConstant}
import io.glassdoor.plugin.manager.PluginManagerConstant

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait Plugin extends Actor {
  private var uniqueId:Option[Long] = None

  def apply(data:Map[String,String], parameters:Array[String])
  def result:Option[Map[String,String]]
  def help(parameters:Array[String])

  //method should be called when the plugin is ready to return a result
  def ready():Unit = {
    val resultData = PluginResult(uniqueId, result)
    EventBus.publish(new MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.pluginResult, Some(resultData))))
    EventBus.publish(new MessageEvent(UserInterfaceConstant.channel, Message(UserInterfaceConstant.Action.taskCompleted, uniqueId)))
  }

  //updates the progress of this task to the specified value
  def showProgress(progress:Double):Unit = {
    //TODO
  }

  def showEndlessProgress():Unit = {
    if(uniqueId.isDefined){
      EventBus.publish(new MessageEvent(UserInterfaceConstant.channel, Message(UserInterfaceConstant.Action.showEndlessProgress, Some(uniqueId.get))))
    }
  }

  override def receive: Receive = {
    case Message(action, data) =>
      action match {
        case PluginConstant.Action.apply =>
          if(data.isDefined){
            val pluginParameters = data.get.asInstanceOf[PluginParameters]
            apply(pluginParameters.data, pluginParameters.parameters)
          }
        case PluginConstant.Action.setUniqueId =>
          if(data.isDefined){
            val id = data.get.asInstanceOf[Long]
            uniqueId = Some(id)
          }
      }
  }
}

object PluginConstant {
  object Action {
    val apply = "apply"
    val help = "help"
    val setUniqueId = "id"
  }
}

case class PluginParameters(data:Map[String, String], parameters:Array[String])
case class PluginResult(uniqueId:Option[Long], result:Option[Map[String,String]])
