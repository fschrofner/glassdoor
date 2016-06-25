package io.glassdoor.plugin

import akka.actor.Actor
import io.glassdoor.application.Context
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.manager.{PluginManagerConstant}

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
    //tell the UI first, that the plugin has completed, before removing it from the running plugins
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginTaskCompleted, uniqueId)))
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginResult, Some(resultData))))
    //EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.TaskCompleted, uniqueId)))
  }

  //updates the progress of this task to the specified value
  def showProgress(progress:Double):Unit = {
    //TODO
  }

  def showEndlessProgress():Unit = {
    //val message = new PluginProgress(uniqueId, )
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginShowEndlessProgress, uniqueId)))
    //EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.ShowEndlessProgress, Some(uniqueId.get))))
  }

  override def receive: Receive = {
    case Message(action, data) =>
      action match {
        case PluginConstant.Action.Apply =>
          if(data.isDefined){
            val pluginParameters = data.get.asInstanceOf[PluginParameters]
            apply(pluginParameters.data, pluginParameters.parameters)
          }
        case PluginConstant.Action.SetUniqueId =>
          if(data.isDefined){
            val id = data.get.asInstanceOf[Long]
            uniqueId = Some(id)
          }
      }
  }
}

object PluginConstant {
  object Action {
    val Apply = "apply"
    val Help = "help"
    val SetUniqueId = "id"
  }
}

case class PluginParameters(data:Map[String, String], parameters:Array[String])
case class PluginResult(uniqueId:Option[Long], result:Option[Map[String,String]])
case class PluginProgress(uniqueId:Option[Long], pluginName:String, progress:Option[Integer])
