package io.glassdoor.controller

import akka.actor.Actor
import akka.actor.Actor.Receive
import io.glassdoor.application.{Command, Configuration, Context, Log}
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.plugin.manager.{PluginErrorMessage, PluginManagerConstant, PluginManagerPluginParameters}
import io.glassdoor.plugin.resource.{ResourceErrorMessage, ResourceManagerConstant, ResourceManagerResourceParameters, ResourceSuccessMessage}
import io.glassdoor.resource.Resource

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 4/17/16.
  */
trait Controller extends Actor {
  protected var mContext:Option[Context] = None

  def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit
  def handleChangedValues(changedValues:Map[String,String]):Unit
  def handleRemovedValues(removedValues:Array[String]):Unit
  def handleInstallResource(names:Array[String])
  def handleRemoveResource(names:Array[String])
  def handlePluginError(pluginInstance:Option[PluginInstance], errorCode:Integer, data:Option[Any])
  def handleResourceError(resource:Option[Resource], errorCode:Integer, data:Option[Any])
  def handleResourceSuccess(resource:Option[Resource], code:Integer)
  def buildAliasIndex(context:Context):Unit
  def handleUpdateAvailableResources():Unit
  def handlePluginTaskCompleted(pluginInstance:PluginInstance):Unit
  def handleContextUpdateRequestByPluginManager():Unit
  def handleWaitForInput():Unit
  def handleUiPrint(message:String):Unit
  def handlePluginHelp(plugin:String):Unit
  def handlePluginList():Unit

  def applyPlugin(pluginName: String, parameters:Array[String]):Unit = {
    if(mContext.isDefined){
      val message = new PluginManagerPluginParameters(pluginName, parameters, mContext.get)
      EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ApplyPlugin, Some(message))))
    }
  }

  def applyPlugins(pluginNames: Array[String], parameters: Array[Array[String]]):Unit = {
    if(mContext.isDefined && pluginNames.length == parameters.length){
      val pluginParameterArray = ArrayBuffer[PluginManagerPluginParameters]()

      for(i <- pluginNames.indices){
        pluginParameterArray.append(PluginManagerPluginParameters(pluginNames(i), parameters(i),mContext.get))
      }

      val message = pluginParameterArray.toArray

      EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ApplyPlugins, Some(message))))
    }
  }

  def forwardWaitForInput():Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.WaitForInput, None)))
  }

  def sendContextUpdateToPluginManager(): Unit ={
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ContextUpdate, mContext)))
  }

  def updateAvailableResources(): Unit ={
    EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.UpdateAvailableResourceIndex,mContext)))
  }

  def installResource(resourceName:String):Unit = {
    if(mContext.isDefined){
      val message = ResourceManagerResourceParameters(resourceName, mContext.get)
      EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.InstallResource, Some(message))))
    }
  }

  def removeResource(resourceName:String):Unit = {
    if(mContext.isDefined){
      val message = ResourceManagerResourceParameters(resourceName, mContext.get)
      EventBus.publish(MessageEvent(ResourceManagerConstant.Channel, Message(ResourceManagerConstant.Action.RemoveResource, Some(message))))
    }
  }

  def forwardPluginErrorMessage(pluginInstance:Option[PluginInstance], error:Int, data:Option[Any]): Unit ={
    val messageData = PluginErrorMessage(pluginInstance, error, data)
    val message = Message(UserInterfaceConstant.Action.PluginError, Some(messageData))
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, message))
  }

  def forwardResourceSuccessMessage(resource:Option[Resource], code:Integer): Unit = {
    val messageData = ResourceSuccessMessage(resource,code)
    val message = Message(UserInterfaceConstant.Action.ResourceSuccess, Some(messageData))
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, message))
  }

  def forwardResourceErrorMessage(resource:Option[Resource], errorCode:Integer, data:Option[Any]): Unit = {
    val messageData = ResourceErrorMessage(resource, errorCode, data)
    val message = Message(UserInterfaceConstant.Action.ResourceError, Some(messageData))
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, message))
  }

  def forwardTaskCompletedMessage(pluginInstance:PluginInstance): Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.TaskCompleted, Some(pluginInstance))))
  }

  def forwardUiPrint(message:String):Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.Print, Some(message))))
  }

  def forwardHelpForPlugin(plugin:String):Unit = {
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ShowPluginHelp, Some(plugin))))
  }

  def forwardPluginList():Unit = {
    EventBus.publish(MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.ShowPluginList, None)))
  }

  def forwardPluginCommandList(commands: Array[String]):Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.PluginCommandList, Some(commands))))
  }

  def forwardAliasList(aliases: Array[String]):Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.AliasList, Some(aliases))))
  }

  def forwardContextKeys(contextKeys: Array[String]):Unit = {
    EventBus.publish(MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.ContextList, Some(contextKeys))))
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

    //TODO: now load the resources into context!

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
        case ControllerConstant.Action.ApplyRemovedValues =>
          if(data.isDefined){
            val removedValues = data.get.asInstanceOf[Array[String]]
            handleRemovedValues(removedValues)
          }
        case ControllerConstant.Action.InstallResource =>
          if(data.isDefined){
            val name = data.get.asInstanceOf[Array[String]]
            handleInstallResource(name)
          }
        case ControllerConstant.Action.RemoveResource =>
          if(data.isDefined){
            val name = data.get.asInstanceOf[Array[String]]
            handleRemoveResource(name)
          }
        case ControllerConstant.Action.PluginError =>
          if(data.isDefined){
            val messageData = data.get.asInstanceOf[PluginErrorMessage]
            handlePluginError(messageData.pluginInstance, messageData.errorCode, messageData.data)
          }
        case ControllerConstant.Action.ResourceSuccess =>
          if(data.isDefined){
            val messageData = data.get.asInstanceOf[ResourceSuccessMessage]
            handleResourceSuccess(messageData.resource, messageData.code)
          }
        case ControllerConstant.Action.ResourceError =>
          if(data.isDefined){
            val messageData = data.get.asInstanceOf[ResourceErrorMessage]
            handleResourceError(messageData.resource, messageData.errorCode, messageData.data)
          }
        case ControllerConstant.Action.UpdateAvailableResources =>
          handleUpdateAvailableResources()
        case ControllerConstant.Action.TaskCompleted =>
          if(data.isDefined){
            val taskInstance = data.get.asInstanceOf[PluginInstance]
            handlePluginTaskCompleted(taskInstance)
          }
        case ControllerConstant.Action.ContextUpdateRequestPluginManager =>
          handleContextUpdateRequestByPluginManager()
        case ControllerConstant.Action.WaitForInput =>
          handleWaitForInput()
        case ControllerConstant.Action.PrintInUi =>
          if(data.isDefined){
            val message = data.get.asInstanceOf[String]
            handleUiPrint(message)
          }
        case ControllerConstant.Action.ShowPluginHelp =>
          if(data.isDefined){
            val plugin = data.get.asInstanceOf[String]
            handlePluginHelp(plugin)
          }
        case ControllerConstant.Action.PluginCommandList =>
          if(data.isDefined){
            val commands = data.get.asInstanceOf[Array[String]]
            forwardPluginCommandList(commands)
          }
        case ControllerConstant.Action.ShowPluginList =>
          handlePluginList()
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
    val RemoveResource = "removeResource"
    val ApplyChangedValues = "applyChangedValues"
    val ApplyRemovedValues = "applyRemovedValues"
    val PluginCommandList = "pluginCommandList"
    val PluginError = "pluginError"
    val ResourceError = "resourceError"
    val ResourceSuccess = "resourceSuccess"
    val UpdateAvailableResources = "updateAvailableResources"
    val TaskCompleted = "pluginTaskCompleted"
    val ContextUpdateRequestPluginManager = "contextUpdateRequestPluginManger"
    val WaitForInput = "waitForInput"
    val PrintInUi = "printInUi"
    val ShowPluginHelp = "showPluginHelp"
    val ShowPluginList = "showPluginList"
  }
}
