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
      //TODO: load plugin from external resource
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
    val file = new File(Constant.CONFIG_PLUGIN_FILE_PATH)
    val config = ConfigFactory.parseFile(file);

    val defaultPluginList = config.getConfigList(Constant.CONFIG_DEFAULT_KEY + "." + Constant.CONFIG_DEFAULT_PLUGIN_KEY).asScala

    for(pluginConfig:Config <- defaultPluginList){
      try {
        val name = pluginConfig.getString(Constant.CONFIG_PLUGIN_NAME_KEY)
        val typ = pluginConfig.getString(Constant.CONFIG_PLUGIN_TYPE_KEY)
        val dependencies = pluginConfig.getStringList(Constant.CONFIG_PLUGIN_DEPENDENCIES_KEY).asScala
        val commands = pluginConfig.getStringList(Constant.CONFIG_PLUGIN_COMMANDS_KEY).asScala
        val className = pluginConfig.getString(Constant.CONFIG_PLUGIN_CLASSFILE_KEY)

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
}
