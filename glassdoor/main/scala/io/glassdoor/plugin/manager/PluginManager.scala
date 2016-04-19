package io.glassdoor.plugin.manager

import akka.actor.Actor
import io.glassdoor.application.Context
import io.glassdoor.bus.Message

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait PluginManager extends Actor {
  def loadPlugin(pluginName:String):Unit
  def unloadPlugin(pluginName:String):Unit
  def findPlugin(pluginName:String):Array[String]
  def buildPluginIndex(context:Context):Unit
  def applyPlugin(pluginName:String,parameters:Array[String],context:Context):Unit

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
      }
  }
}

object PluginManagerConstant {
  val channel = "/pluginManager"
  object Action {
    val buildPluginIndex = "buildPluginIndex"
    val applyPlugin = "applyPlugin"
  }
}

case class PluginManagerPluginParameters(pluginName:String, parameters:Array[String], context:Context)
