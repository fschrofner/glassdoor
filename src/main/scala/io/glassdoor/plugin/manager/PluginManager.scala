package io.glassdoor.plugin.manager

import java.util.Map.Entry

import akka.actor.{Actor, ActorRef}
import io.glassdoor.application.{Context, ContextConstant, Log}
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.resource.ResourceManagerConstant
import io.glassdoor.plugin.{DynamicValues, PluginInstance, PluginResult}

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait PluginManager extends Actor {
  def loadPlugin(pluginName:String):Unit
  def unloadPlugin(pluginName:String):Unit
  def findPlugin(pluginName:String):Array[String]
  def initialise(context:Context):Unit
  def applyPlugin(pluginName:String,parameters:Array[String],context:Context):Unit
  def handlePluginResult(pluginId:Long, changedValues:Map[String,String]):Unit
  def getPluginInstance(pluginId:Long):Option[PluginInstance]
  def handleContextUpdate(context:Context):Unit
  def handleResolvedDynamicValues(dynamicValues:DynamicValues):Unit

  def applyChangedValues(changedValues:Map[String,String]):Unit = {
    val changedResources:scala.collection.mutable.Map[String, String] = new scala.collection.mutable.HashMap[String, String]

    for(changedValue <- changedValues){
      val prefix = changedValue._1.substring(0, changedValue._1.indexOf(ContextConstant.DescriptorSplit))
      if(prefix == ContextConstant.Keymap.Resource){
        changedResources.put(changedValue._1, changedValue._2)
      }
    }

    //additionally notify resource manager of changed resources
    if(changedResources.size > 0){
      Log.debug("changed values containing resources! notifying resource manager..")
      val message = new Message(ResourceManagerConstant.Action.ResourceInstallComplete, Some(changedResources.toMap))
      EventBus.publish(new MessageEvent(ResourceManagerConstant.Channel, message))
    }

    val message = new Message(ControllerConstant.Action.ApplyChangedValues, Some(changedValues))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  def sendErrorMessage(pluginInstance:Option[PluginInstance], errorCode:Integer, data:Option[Any]):Unit = {
    val messageData = new PluginErrorMessage(pluginInstance, errorCode, data)
    val message = new Message(ControllerConstant.Action.PluginError, Some(messageData))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }


  def requestContextUpdate():Unit = {
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ContextUpdateRequestPluginManager,None)))
  }

  override def receive = {
    case Message(action, data) =>
      action match {
        case PluginManagerConstant.Action.Initialise =>
          if(data.isDefined){
            initialise(data.get.asInstanceOf[Context])
          }
        case PluginManagerConstant.Action.ApplyPlugin =>
          if(data.isDefined){
            val parameter = data.get.asInstanceOf[PluginManagerPluginParameters]
            applyPlugin(parameter.pluginName,parameter.parameters, parameter.context)
          }
        case PluginManagerConstant.Action.PluginResult =>
          if(data.isDefined){
            val resultData = data.get.asInstanceOf[PluginResult]
            if(resultData.uniqueId.isDefined && resultData.result.isDefined){
              handlePluginResult(resultData.uniqueId.get, resultData.result.get)
            }
          }
        case PluginManagerConstant.Action.PluginShowEndlessProgress =>
          if(data.isDefined){
            val uniqueId = data.get.asInstanceOf[Long]
            val instance = getPluginInstance(uniqueId)
            if(instance.isDefined){
              EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.ShowEndlessProgress, instance)))
            }
          }
        case PluginManagerConstant.Action.PluginTaskCompleted =>
          if(data.isDefined){
            val uniqueId = data.get.asInstanceOf[Long]
            val instance = getPluginInstance(uniqueId)
            if(instance.isDefined){
              Log.debug("plugin instance found, forwarding!")
              //EventBus.publish(new MessageEvent(UserInterfaceConstant.Channel, Message(UserInterfaceConstant.Action.TaskCompleted, instance)))
              EventBus.publish(new MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.TaskCompleted, instance)))
            } else {
              Log.debug("plugin instance not found!")
            }
          }
        case PluginManagerConstant.Action.ContextUpdate =>
          if(data.isDefined){
            handleContextUpdate(data.get.asInstanceOf[Context])
          }
        case PluginManagerConstant.Action.DynamicValueUpdate =>
          if(data.isDefined){
            val resolvedValues = data.get.asInstanceOf[DynamicValues]
            handleResolvedDynamicValues(resolvedValues)
          }
      }
  }
}

object PluginManagerConstant {
  val Channel = "/pluginManager"
  val DynamicDependency = "dynamic"

  object Action {
    val Initialise = "initialise"
    val ApplyPlugin = "applyPlugin"
    val PluginResult = "pluginResult"
    val PluginShowEndlessProgress = "pluginShowEndlessProgress"
    val PluginShowProgress = "pluginShowProgress"
    val PluginTaskCompleted = "pluginTaskCompleted"
    val ContextUpdate = "contextUpdate"
    val DynamicValueUpdate = "dynamicValueUpdate"
  }

  object PluginErrorCode {
    val DependenciesNotSatisfied = 100
    val DependenciesInChange = 101
    val PluginNotFound = 102
  }
}

case class PluginManagerPluginParameters(pluginName:String, parameters:Array[String], context:Context)
case class PluginErrorMessage(pluginInstance: Option[PluginInstance], errorCode: Integer, data:Option[Any])