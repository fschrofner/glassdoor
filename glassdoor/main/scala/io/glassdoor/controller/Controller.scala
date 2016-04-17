package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Configuration, Context}
import io.glassdoor.bus.{EventBus, MessageEvent, Message}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.{ApplyPluginParameters, PluginManagerConstant}

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  private var mContext:Context = null

  def setup():Unit = {
    Configuration.loadConfig()
    mContext = new Context
    mContext = Configuration.loadConfigIntoContext(mContext)

    //setup other components
    EventBus.publish(MessageEvent(UserInterfaceConstant.channel, Message(UserInterfaceConstant.Action.initialise , Some(mContext))))
    EventBus.publish(MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.buildPluginIndex, Some(mContext))))
  }

  def terminate():Unit = {
    context.system.terminate()
  }

  override def receive = {
    case Message(action, data) =>
      action match {
        case ControllerConstant.Action.setup =>
          setup()
        case ControllerConstant.Action.context =>
          if(data.isDefined){
            mContext = data.get.asInstanceOf[Context]
          }
        case ControllerConstant.Action.terminate =>
          terminate()
        case ControllerConstant.Action.applyPlugin =>
          if(data.isDefined){
            val receivedParameters = data.get.asInstanceOf[PluginParameters]
            val parameters = new ApplyPluginParameters(receivedParameters.pluginName, receivedParameters.parameters, mContext)
            EventBus.publish(MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.applyPlugin, Some(parameters))))
          }
      }
  }

}

object ControllerConstant {
  val channel = "/controller"
  object Action {
    val setup = "setup"
    val context = "context"
    val terminate = "terminate"
    val applyPlugin = "applyPlugin"
  }
}

case class PluginParameters(pluginName:String,parameters:Array[String])