package io.glassdoor.plugin

import java.io.File
import scala.collection.JavaConverters._
import java.util

import com.typesafe.config.{ConfigException, Config, ConfigFactory}
import io.glassdoor.application.{Context, Constant, Configuration}

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/30/16.
  */
class DefaultPluginManager extends PluginManager{
  //a list with the loaded plugins and their key values
  var mLoadedPlugins:HashMap[String, PluginInstance] = new HashMap[String,PluginInstance]

  //this should contain all plugins found in the plugin directory
  var mPluginMap:Map[String, String] = new HashMap[String, String]

  override def loadPlugin(pluginName: String): Unit = ???

  override def unloadPlugin(pluginName: String): Unit = ???

  override def findPlugin(pluginName: String): Array[String] = ???

  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    var pluginInstance:Option[PluginInstance] = None

    if(mLoadedPlugins.contains(pluginName)){
       pluginInstance = mLoadedPlugins.get(pluginName)
    } else {
      //TODO: load plugin from external resource or show error
    }

    if(pluginInstance.isDefined){
      //TODO: check dependencies
      val plugin = pluginInstance.get.plugin
      plugin.apply(context,parameters)
    }
  }

  override def buildPluginIndex(): Unit = {
    loadDefaultPlugins()
  }

  def loadDefaultPlugins():Unit = {
    //TODO: load default plugins into hashmap
    val file = new File(Constant.Config.Path.PLUGIN_CONFIG_FILE)
    val config = ConfigFactory.parseFile(file);

    val defaultPluginList = config.getConfigList(Constant.Config.ConfigKey.FullKey.DEFAULT_PLUGINS).asScala

    for(pluginConfig:Config <- defaultPluginList){
      try {
        val name = pluginConfig.getString(Constant.Config.PluginKey.NAME)
        val typ = pluginConfig.getString(Constant.Config.PluginKey.TYPE)
        val dependencies = pluginConfig.getStringList(Constant.Config.PluginKey.DEPENDENCIES).asScala
        val commands = pluginConfig.getStringList(Constant.Config.PluginKey.COMMANDS).asScala
        val className = pluginConfig.getString(Constant.Config.PluginKey.CLASSFILE)

        //instantiate the class
        val plugin = instantiateDefaultPlugin(className)

        val pluginInstance = new PluginInstance(name,typ,dependencies.toArray,commands.toArray,plugin)
        mLoadedPlugins += ((pluginInstance.name,pluginInstance))

        println("plugin detected: " + name)
      } catch {
        case e:ConfigException =>
          println("plugin information missing!")
      }

    }
  }

  def instantiateDefaultPlugin(className:String):Plugin = {
    Class.forName(className).newInstance().asInstanceOf[Plugin]
  }

  override def getPluginResult(pluginName: String): Option[Context] = {
    var pluginInstance:Option[PluginInstance] = None

    if(mLoadedPlugins.contains(pluginName)){
      pluginInstance = mLoadedPlugins.get(pluginName)
    } else {
      //TODO: load plugin from external resource or show error
      return None
    }

    if(pluginInstance.isDefined){
      return Some(pluginInstance.get.plugin.result)
    } else {
      return None
    }
  }
}
