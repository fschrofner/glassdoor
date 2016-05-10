package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Configuration, Context, Command}
import io.glassdoor.bus.{EventBus, MessageEvent, Message}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.manager.{PluginManagerPluginParameters, PluginManagerConstant}
import io.glassdoor.plugin.resource.{ResourceManagerConstant, ResourceManagerResourceParameters}

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  protected var mContext:Context = null

  def applyPlugin(pluginName: String, parameters:Array[String]):Unit = {
    val message = new PluginManagerPluginParameters(pluginName, parameters, mContext)
    EventBus.publish(MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.applyPlugin, Some(message))))
  }

  def installResource(resourceName:String):Unit = {
    val message = new ResourceManagerResourceParameters(resourceName, mContext)
    EventBus.publish(MessageEvent(ResourceManagerConstant.channel, Message(ResourceManagerConstant.Action.installResource, Some(message))))
  }

  def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit
  def handleChangedValues(changedValues:Map[String,String]):Unit
  def handleInstallResource(names:Array[String])
  def buildAliasIndex(context:Context):Unit

  def setup():Unit = {
    Configuration.loadConfig()
    mContext = new Context

    val contextOpt = Configuration.loadConfigIntoContext(mContext)

    if(contextOpt.isDefined){
      mContext = contextOpt.get
    } else {
      //TODO: error handling
    }

    buildAliasIndex(mContext)
    //setup other components
    EventBus.publish(MessageEvent(UserInterfaceConstant.channel, Message(UserInterfaceConstant.Action.initialise , Some(mContext))))
    EventBus.publish(MessageEvent(PluginManagerConstant.channel, Message(PluginManagerConstant.Action.buildPluginIndex, Some(mContext))))
    EventBus.publish(MessageEvent(ResourceManagerConstant.channel, Message(ResourceManagerConstant.Action.buildResourceIndex, Some(mContext))))
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
            val receivedParameters = data.get.asInstanceOf[Command]
            handleApplyPlugin(receivedParameters.name,receivedParameters.parameters)
          }
        case ControllerConstant.Action.applyChangedValues =>
          if(data.isDefined){
            val changedValues = data.get.asInstanceOf[Map[String,String]]
            handleChangedValues(changedValues)
          }

        case ControllerConstant.Action.installResource =>
          if(data.isDefined){
            val name = data.get.asInstanceOf[Array[String]]
            handleInstallResource(name)
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
    val installResource = "installResource"
    val applyChangedValues = "applyChangedValues"
  }
}
