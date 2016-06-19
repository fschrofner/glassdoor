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
  def buildPluginIndex(context:Context):Unit
  def applyPlugin(pluginName:String,parameters:Array[String],context:Context):Unit
  def handlePluginResult(pluginId:Long, changedValues:Map[String,String]):Unit

  def applyChangedValues(changedValues:Map[String,String]):Unit = {
    val message = new Message(ControllerConstant.Action.applyChangedValues, Some(changedValues))
    EventBus.publish(new MessageEvent(ControllerConstant.channel, message))
  }

  def sendErrorMessage(pluginId:Long, errorCode:Integer, data:Option[Any]):Unit = {
    val messageData = new PluginErrorMessage(pluginId, errorCode, data)
    val message = new Message(ControllerConstant.Action.pluginError, Some(messageData))
    EventBus.publish(new MessageEvent(ControllerConstant.channel, message))
  }

  override def receive = {
    case Message(action, data) =>
      action match {
        case PluginManagerConstant.Action.buildPluginIndex =>
          if(data.isDefined){
            buildPluginIndex(data.get.asInstanceOf[Context])
          }
        case PluginManagerConstant.Action.applyPlugin =>
          if(data.isDefined){
            val parameter = data.get.asInstanceOf[PluginManagerPluginParameters]
            applyPlugin(parameter.pluginName,parameter.parameters, parameter.context)
          }
        case PluginManagerConstant.Action.pluginResult =>
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
  val channel = "/pluginManager"
  object Action {
    val buildPluginIndex = "buildPluginIndex"
    val applyPlugin = "applyPlugin"
    val pluginResult = "pluginResult"
  }

  object PluginErrorCodes {
    val dependenciesNotSatisfied = 100
    val dependenciesInChange = 101
    val pluginNotFound = 102
  }
}

case class PluginManagerPluginParameters(pluginName:String, parameters:Array[String], context:Context)
case class PluginErrorMessage(pluginId: Long, errorCode: Integer, data:Option[Any])