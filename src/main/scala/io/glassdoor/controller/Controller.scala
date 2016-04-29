package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Configuration, Context}
import io.glassdoor.bus.{EventBus, MessageEvent, Message}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.manager.{PluginManagerPluginParameters, PluginManagerConstant}

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  protected var mContext:Context = null

  def handleChangedValues(changedValues:Map[String,String]):Unit

  def setup():Unit = {
    Configuration.loadConfig()
    mContext = new Context

    val contextOpt = Configuration.loadConfigIntoContext(mContext)

    if(contextOpt.isDefined){
      mContext = contextOpt.get
    } else {
      //TODO: error handling
    }

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
            val receivedParameters = data.get.asInstanceOf[ControllerPluginParameters]
            val parameters = new PluginManagerPluginParameters(receivedParameters.pluginName, receivedParameters.parameters, mContext)
            EventBus.publish(MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.applyPlugin, Some(parameters))))
          }

        case ControllerConstant.Action.applyChangedValues =>
          if(data.isDefined){
            val changedValues = data.get.asInstanceOf[Map[String,String]]
            handleChangedValues(changedValues)
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
    val applyChangedValues = "applyChangedValues"
  }
}

case class ControllerPluginParameters(pluginName:String, parameters:Array[String])
