package io.glassdoor.plugin

import akka.actor.Actor
import io.glassdoor.application.Context
import io.glassdoor.bus.{MessageEvent, EventBus, Message}
import io.glassdoor.plugin.manager.PluginManagerConstant

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait Plugin extends Actor {
  def apply(data:Map[String,String], parameters:Array[String])
  def result:Option[Map[String,String]]
  def help(parameters:Array[String])

  //method should be called when the plugin is ready to return a result
  def ready():Unit = {
    EventBus.publish(new MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.pluginResult, result)))
  }

  override def receive: Receive = {
    case Message(action, data) =>
      action match {
        case PluginConstant.Action.apply =>
          if(data.isDefined){
            val pluginParameters = data.get.asInstanceOf[PluginParameters]
            apply(pluginParameters.data, pluginParameters.parameters)
          }
      }
  }
}

object PluginConstant {
  object Action {
    val apply = "apply"
    val help = "help"
  }
}

case class PluginParameters(data:Map[String, String], parameters:Array[String])