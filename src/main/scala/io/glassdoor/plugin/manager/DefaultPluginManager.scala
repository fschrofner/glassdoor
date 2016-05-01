package io.glassdoor.plugin.manager

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import groovy.lang.GroovyClassLoader
import io.glassdoor.application._
import io.glassdoor.bus.Message
import io.glassdoor.plugin.language.GroovyPlugin
import io.glassdoor.plugin.{PluginParameters, PluginConstant, Plugin, PluginInstance, PluginResult, PluginData}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by Florian Schrofner on 3/30/16.
  */
class DefaultPluginManager extends PluginManager{

  //contains all currently running plugins, unique id as key value
  var mRunningPlugins:scala.collection.mutable.Map[Long,PluginInstance] = new scala.collection.mutable.HashMap[Long, PluginInstance]

  //a list for plugins currently waiting to be executed
  var mPluginQueue:List[PluginInstance] = List[PluginInstance]()

  //a map of the loaded plugins, plugin names are the key values
  var mLoadedPlugins:Map[String, PluginData] = new HashMap[String,PluginData]

  override def loadPlugin(pluginName: String): Unit = ???

  override def unloadPlugin(pluginName: String): Unit = ???

  override def findPlugin(pluginName: String): Array[String] = ???

  override def handlePluginResult(pluginId:Long, changedValues:Map[String,String]):Unit = {
    //TODO: check if permissions are met
    //TODO: remove keymaps in change and reduce dependency counter

    var matchingPlugin:Option[PluginInstance] = None

    if(mRunningPlugins.contains(pluginId)){
      matchingPlugin = mRunningPlugins.get(pluginId)
    }

    if(matchingPlugin.isDefined){
      val pluginInstance = matchingPlugin.get

      println("received result from: " + pluginInstance.name)

      for((key,value) <- changedValues){
        if(pluginInstance.changes.contains(key)){
          println("correctly changed key: " + key)
        } else {
          println("error: change not specified in manifest!")
        }
      }

      applyChangedValues(changedValues)
      mRunningPlugins.remove(pluginId)

      //TODO: start next (launchable) plugin in queue

    } else {
      println("no matching plugin found!")
    }
  }

  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    //TODO: only send the data the plugin needs
    var pluginDataOpt:Option[PluginData] = None

    if(mLoadedPlugins.contains(pluginName)){
      pluginDataOpt = mLoadedPlugins.get(pluginName)
    } else {
      //TODO: error, plugin not found
    }

    if(pluginDataOpt.isDefined){
      //TODO: check if dependencies satisfied
      //TODO: check that no two plugins can run at the same time, which will work with the same data

      val pluginData = pluginDataOpt.get
      val dependencies = pluginData.dependencies
      val changes = pluginData.changes

      val mutableHashmap = new mutable.HashMap[String,String]

      //provide access to the dependencies
      for(dependency <- dependencies){
        val value = context.getResolvedValue(dependency)
        if(value.isDefined){
          mutableHashmap.put(dependency, value.get)
        }
      }

      //provide access to values it changes
      for(change <- changes){
        val value = context.getResolvedValue(change)
        if(value.isDefined){
          mutableHashmap.put(change, value.get)
        }
      }

      //provide configuration in context
      val configKeymapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.CONFIG)

      if(configKeymapOpt.isDefined){
        val configKeymap = configKeymapOpt.get

        for((key,value) <- configKeymap){
          val fullKey = ContextConstant.Keymap.CONFIG + ContextConstant.DESCRIPTOR_SPLIT + key
          mutableHashmap.put(fullKey,value)
        }
      }

      val actor = instantiatePlugin(pluginData.pluginClass, pluginData.pluginEnvironment)

      //if plugin instantiation successful
      if(actor.isDefined){
        val id = UniqueIdGenerator.generate()

        actor.get ! Message(PluginConstant.Action.setUniqueId, Some(id))

        //create a new plugin instance with the data
        val pluginInstance = PluginInstance(id, pluginData.name, pluginData.kind, pluginData.dependencies, pluginData.changes, pluginData.commands, actor.get)

        mRunningPlugins.put(pluginInstance.uniqueId, pluginInstance)

        //apply plugin
        actor.get ! Message(PluginConstant.Action.apply, Some(new PluginParameters(mutableHashmap.toMap[String,String], parameters)))
      }
    }
  }

  def instantiatePlugin(pluginClass:String, pluginEnvironment:Option[Map[String, String]] = None):Option[ActorRef] = {
    val targetClass = Class.forName(pluginClass)
    val actor = context.system.actorOf(Props(targetClass))

    if(pluginEnvironment.isDefined){
      //TODO: send environment to plugin, e.g. path to groovy script
    }

    Some(actor)
  }

  override def buildPluginIndex(context:Context): Unit = {
    loadDefaultPlugins(context)
  }

  def loadDefaultPlugins(context:Context):Unit = {
    val pluginConfigPath = context.getResolvedValue(ContextConstant.FullKey.CONFIG_PLUGIN_CONFIG_PATH)

    if(pluginConfigPath.isDefined){
      val file = new File(pluginConfigPath.get)
      val config = ConfigFactory.parseFile(file)

      val defaultPluginList = config.getConfigList(ConfigConstant.ConfigKey.FullKey.DEFAULT_PLUGINS).asScala

      for(pluginConfig:Config <- defaultPluginList){
        try {
          val name = pluginConfig.getString(ConfigConstant.PluginKey.NAME)
          val typ = pluginConfig.getString(ConfigConstant.PluginKey.TYPE)
          val dependencies = pluginConfig.getStringList(ConfigConstant.PluginKey.DEPENDENCIES).asScala
          val changes = pluginConfig.getStringList(ConfigConstant.PluginKey.CHANGES).asScala
          val commands = pluginConfig.getStringList(ConfigConstant.PluginKey.COMMANDS).asScala
          val pluginClass = pluginConfig.getString(ConfigConstant.PluginKey.CLASSFILE)
          val pluginEnvironment = None

          val pluginData = new PluginData(name,typ,dependencies.toArray,changes.toArray, commands.toArray, pluginClass, pluginEnvironment)

          mLoadedPlugins += ((pluginData.name, pluginData))

          println("plugin detected: " + name)
        } catch {
          case e:ConfigException =>
            println("plugin information missing!")
        }

      }
    }
  }

  def instantiateDefaultPlugin(className:String):ActorRef = {
    val pluginClass = Class.forName(className)
    context.system.actorOf(Props(pluginClass))
  }
}
