package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Log, Configuration, Context, Command}
import io.glassdoor.bus.{EventBus, MessageEvent, Message}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.manager.{PluginErrorMessage, PluginManagerPluginParameters, PluginManagerConstant}
import io.glassdoor.plugin.resource.{ResourceManagerConstant, ResourceManagerResourceParameters}

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  protected var mContext:Context = null

  def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit
  def handleChangedValues(changedValues:Map[String,String]):Unit
  def handleInstallResource(names:Array[String])
  def handlePluginError(pluginId:Long, errorCode:Integer, data:Option[Any])
  def buildAliasIndex(context:Context):Unit

  def applyPlugin(pluginName: String, parameters:Array[String]):Unit = {
    val message = new PluginManagerPluginParameters(pluginName, parameters, mContext)
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ApplyPlugin, Some(message))))
  }

  def installResource(resourceName:String):Unit = {
    val message = new ResourceManagerResourceParameters(resourceName, mContext)
    EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.InstallResource, Some(message))))
  }

  def forwardErrorMessage(pluginId:Long, errorCode:Integer, data:Option[Any]): Unit ={
    val messageData = new PluginErrorMessage(pluginId, errorCode, data)
    val message = new Message(UserInterfaceConstant.Action.PluginError, Some(messageData))
    EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, message))
  }

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
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.Initialise , Some(mContext))))
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.BuildPluginIndex, Some(mContext))))
    EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.BuildResourceIndex, Some(mContext))))
  }

  def terminate():Unit = {
    Log.debug("controller terminating application..")
    context.system.terminate()
  }

  override def receive = {
    case Message(action, data) =>
      action match {
        case ControllerConstant.Action.Setup =>
          setup()
        case ControllerConstant.Action.Context =>
          if(data.isDefined){
            mContext = data.get.asInstanceOf[Context]
          }
        case ControllerConstant.Action.Terminate =>
          terminate()
        case ControllerConstant.Action.ApplyPlugin =>
          if(data.isDefined){
            val receivedParameters = data.get.asInstanceOf[Command]
            handleApplyPlugin(receivedParameters.name,receivedParameters.parameters)
          }
        case ControllerConstant.Action.ApplyChangedValues =>
          if(data.isDefined){
            val changedValues = data.get.asInstanceOf[Map[String,String]]
            handleChangedValues(changedValues)
          }
        case ControllerConstant.Action.InstallResource =>
          if(data.isDefined){
            val name = data.get.asInstanceOf[Array[String]]
            handleInstallResource(name)
          }
        case ControllerConstant.Action.PluginError =>
          if(data.isDefined){
            val messageData = data.get.asInstanceOf[PluginErrorMessage]
            handlePluginError(messageData.pluginId, messageData.errorCode, messageData.data)
          }
      }
  }

}

object ControllerConstant {
  val Channel = "/controller"
  object Action {
    val Setup = "setup"
    val Context = "context"
    val Terminate = "terminate"
    val ApplyPlugin = "applyPlugin"
    val InstallResource = "installResource"
    val ApplyChangedValues = "applyChangedValues"
    val PluginError = "pluginError"
  }
}
