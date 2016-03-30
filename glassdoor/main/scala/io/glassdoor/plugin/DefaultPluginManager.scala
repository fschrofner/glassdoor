package io.glassdoor.plugin

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/30/16.
  */
class DefaultPluginManager extends PluginManager{
  var pluginMap:Map[String, String] = new HashMap[String, String]

  override def loadPlugin(pluginName: String): Unit = ???

  override def unloadPlugin(pluginName: String): Unit = ???

  override def applyPlugin(pluginName: String): Unit = ???

  override def findPlugin(pluginName: String): Array[String] = ???

  override def buildPluginIndex(): Unit = {

  }

  def loadDefaultPlugins():Map[String,String] ={
    var map = new HashMap[String,String]
    //mContext.originalBinary += ((Constant.ORIGINAL_BINARY_APK,path))
    //TODO: load default plugins into hashmap
    map
  }
}
