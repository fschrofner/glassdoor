package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Command, Configuration, Context, Log}
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.plugin.manager.{PluginErrorMessage, PluginManagerConstant, PluginManagerPluginParameters}
import io.glassdoor.plugin.resource.{ResourceErrorMessage, ResourceManagerConstant, ResourceManagerResourceParameters}
import io.glassdoor.resource.Resource

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  protected var mContext:Option[Context] = None

  def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit
  def handleChangedValues(changedValues:Map[String,String]):Unit
  def handleInstallResource(names:Array[String])
  def handlePluginError(pluginInstance:Option[PluginInstance], errorCode:Integer, data:Option[Any])
  def handleResourceError(resource:Option[Resource], errorCode:Integer, data:Option[Any])
  def buildAliasIndex(context:Context):Unit
  def handleUpdateAvailableResources():Unit

  def applyPlugin(pluginName: String, parameters:Array[String]):Unit = {
    if(mContext.isDefined){
      val message = new PluginManagerPluginParameters(pluginName, parameters, mContext.get)
      EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ApplyPlugin, Some(message))))
    }
  }

  def updateAvailableResources(): Unit ={
    EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.UpdateAvailableResourceIndex,mContext)))
  }

  def installResource(resourceName:String):Unit = {
    if(mContext.isDefined){
      val message = new ResourceManagerResourceParameters(resourceName, mContext.get)
      EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.InstallResource, Some(message))))
    }
  }

  def forwardPluginErrorMessage(pluginInstance:Option[PluginInstance], error:Int, data:Option[Any]): Unit ={
    val messageData = new PluginErrorMessage(pluginInstance, error, data)
    val message = new Message(UserInterfaceConstant.Action.PluginError, Some(messageData))
    EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, message))
  }

  def forwardResourceErrorMessage(resource:Option[Resource], errorCode:Integer, data:Option[Any]): Unit = {
    val messageData = new ResourceErrorMessage(resource, errorCode, data)
    val message = new Message(UserInterfaceConstant.Action.ResourceError, Some(messageData))
    EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, message))
  }

  def setup():Unit = {
    Configuration.loadConfig()
    mContext = Some(new Context)

    val contextOpt = Configuration.loadConfigIntoContext(mContext.get)

    if(contextOpt.isDefined){
      mContext = contextOpt
      buildAliasIndex(mContext.get)
    } else {
      //TODO: error handling
    }

    //setup other components
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.Initialise , mContext)))
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.Initialise, mContext)))
    EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.Initialise, mContext)))
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
            mContext = Some(data.get.asInstanceOf[Context])
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
            handlePluginError(messageData.pluginInstance, messageData.errorCode, messageData.data)
          }
        case ControllerConstant.Action.ResourceError =>
          if(data.isDefined){
            val messageData = data.get.asInstanceOf[ResourceErrorMessage]
            handleResourceError(messageData.resource, messageData.errorCode, messageData.data)
          }
        case ControllerConstant.Action.UpdateAvailableResources =>
          handleUpdateAvailableResources()
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
    val ResourceError = "resourceError"
    val UpdateAvailableResources = "updateAvailableResources"
  }
}
