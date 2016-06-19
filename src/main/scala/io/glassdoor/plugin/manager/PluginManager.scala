package io.glassdoor.plugin.manager

import java.util.Map.Entry

import akka.actor.{Actor, ActorRef}
import io.glassdoor.application.Context
import io.glassdoor.bus.{Message,EventBus, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.{PluginInstance, PluginResult}

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

  def applyChangedValues(changedValues:Map[String,String]):Unit = {
    val message = new Message(ControllerConstant.Action.ApplyChangedValues, Some(changedValues))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  def sendErrorMessage(pluginId:Long, errorCode:Integer, data:Option[Any]):Unit = {
    val messageData = new PluginErrorMessage(pluginId, errorCode, data)
    val message = new Message(ControllerConstant.Action.PluginError, Some(messageData))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
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
      }
  }
}

object PluginManagerConstant {
  val Channel = "/pluginManager"
  object Action {
    val Initialise = "initialise"
    val ApplyPlugin = "applyPlugin"
    val PluginResult = "pluginResult"
  }

  object PluginErrorCodes {
    val DependenciesNotSatisfied = 100
    val DependenciesInChange = 101
    val PluginNotFound = 102
  }
}

case class PluginManagerPluginParameters(pluginName:String, parameters:Array[String], context:Context)
case class PluginErrorMessage(pluginId: Long, errorCode: Integer, data:Option[Any])