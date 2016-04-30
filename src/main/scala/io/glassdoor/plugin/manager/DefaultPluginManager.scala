package io.glassdoor.plugin.manager

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import groovy.lang.GroovyClassLoader
import io.glassdoor.application._
import io.glassdoor.bus.Message
import io.glassdoor.plugin.language.GroovyPlugin
import io.glassdoor.plugin.{PluginParameters, PluginConstant, Plugin, PluginInstance, PluginResult}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by Florian Schrofner on 3/30/16.
  */
class DefaultPluginManager extends PluginManager{
  //a map of the loaded plugins and their key values
  var mLoadedPlugins:Map[String, PluginInstance] = new HashMap[String,PluginInstance]

  //this should contain all plugins found in the plugin directory
  var mPluginMap:Map[String, String] = new HashMap[String, String]

  override def loadPlugin(pluginName: String): Unit = ???

  override def unloadPlugin(pluginName: String): Unit = ???

  override def findPlugin(pluginName: String): Array[String] = ???

  override def handlePluginResult(pluginId:Long, changedValues:Map[String,String]):Unit = {
    //TODO: check if permissions are met
    //TODO: remove from running plugin list
    //TODO: remove keymaps in change and reduce dependency counter
    //TODO: find plugin instance by pluginRef

    var matchingPlugin:Option[PluginInstance] = None

    for((key, pluginInstance) <- mLoadedPlugins){
      if(pluginInstance.uniqueId == pluginId){
        matchingPlugin = Some(pluginInstance)
      }
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

    } else {
      println("no matching plugin found!")
    }
  }

  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    //TODO: only send the data the plugin needs
    var pluginInstance:Option[PluginInstance] = None

    if(mLoadedPlugins.contains(pluginName)){
       pluginInstance = mLoadedPlugins.get(pluginName)
    } else {
      //TODO: differentiate programming languages, handover parameters
      // println("received unknown command, instantiating and calling groovy plugin!")
      // val actor = this.context.system.actorOf(Props[GroovyPlugin])
      // actor ! "testMessage"
    }

    if(pluginInstance.isDefined){
      //TODO: check if dependencies satisfied
      //TODO: check that no two plugins can run at the same time, which will work with the same data

      val plugin = pluginInstance.get.plugin
      val dependencies = pluginInstance.get.dependencies
      val changes = pluginInstance.get.changes

      val mutableHashmap = new mutable.HashMap[String,String]()

      //provide access to the dependencies
      for(dependency <- dependencies){
        val value = context.getResolvedValue(dependency)
        if(value.isDefined){
          mutableHashmap.put(dependency, value.get)
        }
      }

      //provide access 
      for(change <- changes){
        val value = context.getResolvedValue(change)
        if(value.isDefined){
          mutableHashmap.put(change, value.get)
        }
      }

      val configKeymapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.CONFIG)

      if(configKeymapOpt.isDefined){
        val configKeymap = configKeymapOpt.get

        for((key,value) <- configKeymap){
          val fullKey = ContextConstant.Keymap.CONFIG + ContextConstant.DESCRIPTOR_SPLIT + key
          mutableHashmap.put(fullKey,value)
        }
      }

      //TODO: put config inside as well
      //mutableHashmap.put(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY, "/home/flosch/glassdoor")

      //TODO: make sure that the plugins return values!! send value to plugin manager, which checks for permission, then forwards value
      plugin ! Message(PluginConstant.Action.apply, Some(new PluginParameters(mutableHashmap.toMap[String,String], parameters)))
    }
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
          val uniqueId = UniqueIdGenerator.generate()
          val name = pluginConfig.getString(ConfigConstant.PluginKey.NAME)
          val typ = pluginConfig.getString(ConfigConstant.PluginKey.TYPE)
          val dependencies = pluginConfig.getStringList(ConfigConstant.PluginKey.DEPENDENCIES).asScala
          val changes = pluginConfig.getStringList(ConfigConstant.PluginKey.CHANGES).asScala
          val commands = pluginConfig.getStringList(ConfigConstant.PluginKey.COMMANDS).asScala
          val className = pluginConfig.getString(ConfigConstant.PluginKey.CLASSFILE)

          //instantiate the class
          val plugin = instantiateDefaultPlugin(className)

          //setting the unique id for the plugin
          println(name + ":" + uniqueId)
          plugin ! Message(PluginConstant.Action.setUniqueId, Some(uniqueId))

          val pluginInstance = new PluginInstance(uniqueId,name,typ,dependencies.toArray,changes.toArray, commands.toArray, plugin)
          mLoadedPlugins += ((pluginInstance.name,pluginInstance))

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
