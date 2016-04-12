package io.glassdoor.plugin

import io.glassdoor.application.Context

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait PluginManager {
  def loadPlugin(pluginName:String):Unit
  def unloadPlugin(pluginName:String):Unit
  def findPlugin(pluginName:String):Array[String]
  def buildPluginIndex():Unit
  def applyPlugin(pluginName:String,parameters:Array[String],context:Context):Unit
  def getPluginResult(pluginName:String):Option[Context]
}
