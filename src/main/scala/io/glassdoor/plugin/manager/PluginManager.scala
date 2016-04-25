package io.glassdoor.plugin.manager

import java.util.Map.Entry

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
        case PluginManagerConstant.Action.pluginResult =>
          if(data.isDefined){
            //TODO: check if permissions are met
            //TODO: remove from running plugin list
            //TODO: remove keymaps in change and reduce dependency counter
            val changedValues = data.get.asInstanceOf[Map[String,String]]
            for(value <- changedValues){
              println("changing value for key: " + value._1)
              println("changed value: " + value._2)
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
}

case class PluginManagerPluginParameters(pluginName:String, parameters:Array[String], context:Context)
