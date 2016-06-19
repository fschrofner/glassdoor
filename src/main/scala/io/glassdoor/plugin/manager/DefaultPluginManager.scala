package io.glassdoor.plugin.manager

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import groovy.lang.GroovyClassLoader
import io.glassdoor.application._
import io.glassdoor.bus.Message
import io.glassdoor.plugin.language.GroovyPlugin
import io.glassdoor.plugin.manager.PluginManagerConstant.PluginErrorCodes
import io.glassdoor.plugin.{PluginParameters, PluginConstant, Plugin, PluginInstance, PluginResult, PluginData}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.io.Source
import scala.util.Random

/**
  * Created by Florian Schrofner on 3/30/16.
  */
class DefaultPluginManager extends PluginManager{

  //contains all currently running plugins, unique id as key value
  var mRunningPlugins:scala.collection.mutable.Map[Long,PluginInstance] = new scala.collection.mutable.HashMap[Long, PluginInstance]

  //a list of the dependencies of the currently executed plugins with a counter of how many plugins depend on this value (there can be multiple plugins using the same value). it is not allowed for plugins to change values that other plugins depend on.
  var mWorkedOnDependencies:scala.collection.mutable.Map[String, Integer] = new scala.collection.mutable.HashMap[String, Integer]

  //a list of the values that are currently being changed. there must not be more than one plugin working on a value!
  var mChangingValues:ArrayBuffer[String] = ArrayBuffer[String]()

  //a list for plugins currently waiting to be executed
  var mPluginQueue:ArrayBuffer[PluginInstance] = ArrayBuffer[PluginInstance]()

  //a map of the loaded plugins, plugin names are the key values
  var mLoadedPlugins:Map[String, PluginData] = new scala.collection.immutable.HashMap[String,PluginData]

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

      Log.debug("received result from: " + pluginInstance.name)

      for((key,value) <- changedValues){
        if(pluginInstance.changes.contains(key)){
          Log.debug("correctly changing key: " + key)
        } else {
          Log.debug("error: change not specified in manifest!")
        }
      }

      applyChangedValues(changedValues)
      
      clearChangingValuesAndDependencies(pluginInstance)
      mRunningPlugins.remove(pluginId)

      //TODO: start next (launchable) plugin in queue

    } else {
      Log.debug("no matching plugin found!")
    }
  }

  def clearChangingValuesAndDependencies(pluginInstance:PluginInstance):Unit = {
    val changes = pluginInstance.changes
    val dependencies = pluginInstance.dependencies

    for(change <- changes){
      mChangingValues.remove(mChangingValues.indexOf(change))
    }

    for(dependency <- dependencies){
      val prevVal = mWorkedOnDependencies.get(dependency).get
      if(prevVal > 1){
        mWorkedOnDependencies.put(dependency, prevVal - 1)
      } else {
        mWorkedOnDependencies.remove(dependency)
      }
    }
  }



  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    //TODO: only send the data the plugin needs
    //TODO: split up into different methods (checkDependencies, createContext)

    Log.debug("plugin manager: apply plugin called!")

    var pluginDataOpt:Option[PluginData] = None

    if(mLoadedPlugins.contains(pluginName)){
      pluginDataOpt = mLoadedPlugins.get(pluginName)
    } else {
      Log.debug("error: plugin not found!")
      sendErrorMessage(-1, PluginErrorCodes.PluginNotFound, None)
      //TODO: error, plugin not found
    }

    if(pluginDataOpt.isDefined){
      //TODO: check if dependencies satisfied
      //TODO: check that no two plugins can run at the same time, which will work with the same datan

      val pluginData = pluginDataOpt.get
      val dependencies = pluginData.dependencies
      val changes = pluginData.changes

      val mutableHashmap = new scala.collection.mutable.HashMap[String,String]

      //provide access to the dependencies and add them to the current dependencies
      for(dependency <- dependencies){
        val value = context.getResolvedValue(dependency)
        if(value.isDefined) {
          if (mChangingValues.contains(dependency)) {
            Log.debug("dependency in change! can not safely launch plugin!")
            //TODO: there is no plugin id yet, so it can not be supplied with the message
            sendErrorMessage(-1, PluginManagerConstant.PluginErrorCodes.DependenciesInChange, Some(dependency))
            return
          } else {
            if (mWorkedOnDependencies.contains(dependency)) {
              val prevVal = mWorkedOnDependencies.get(dependency).get
              mWorkedOnDependencies.put(dependency, prevVal + 1)
            } else {
              mWorkedOnDependencies.put(dependency, 1)
            }
            mutableHashmap.put(dependency, value.get)
          }
        } else {
          //there might be multiple dependencies, that are not satisfied, but it already stops at the first mismatch
          sendErrorMessage(-1, PluginManagerConstant.PluginErrorCodes.DependenciesNotSatisfied, Some(dependency))
          Log.debug("dependency: " + dependency + " not satisfied!")
          return
        }
      }

      //provide access to values it changes and add value to changed values
      for(change <- changes){
        if(mWorkedOnDependencies.contains(change) || mChangingValues.contains(change)){
          //TODO: can not work on values that other plugins need/ already work on
          Log.debug("value is depended on or already changing! can not safely launch plugin!")
        } else {

          mChangingValues.append(change)

          val value = context.getResolvedValue(change)
          if(value.isDefined){
            mutableHashmap.put(change, value.get)
          }
        }
      }

      //provide configuration in context
      val configKeymapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.Config)

      if(configKeymapOpt.isDefined){
        val configKeymap = configKeymapOpt.get

        for((key,value) <- configKeymap){
          val fullKey = ContextConstant.Keymap.Config + ContextConstant.DescriptorSplit + key
          mutableHashmap.put(fullKey,value)
        }
      }

      val actor = instantiatePlugin(pluginData.pluginClass, pluginData.pluginEnvironment)

      //if plugin instantiation successful
      if(actor.isDefined){
        val id = UniqueIdGenerator.generate()

        Log.debug("starting plugin " + pluginData.name + " with id: " + id)

        actor.get ! Message(PluginConstant.Action.SetUniqueId, Some(id))

        //create a new plugin instance with the data
        val pluginInstance = PluginInstance(id, pluginData.name, pluginData.kind, pluginData.dependencies, pluginData.changes, pluginData.commands, actor.get)

        mRunningPlugins.put(pluginInstance.uniqueId, pluginInstance)

        //apply plugin
        actor.get ! Message(PluginConstant.Action.Apply, Some(new PluginParameters(mutableHashmap.toMap[String,String], parameters)))
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

  override def initialise(context:Context): Unit = {
    loadDefaultPlugins(context)
    //TODO: load plugins available for download
  }

  def loadDefaultPlugins(context:Context):Unit = {
    val pluginConfigPath = context.getResolvedValue(ContextConstant.FullKey.ConfigPluginConfigPath)

    if(pluginConfigPath.isDefined){
      val file = new File(pluginConfigPath.get)
      val config = ConfigFactory.parseFile(file)

      val defaultPluginList = config.getConfigList(ConfigConstant.ConfigKey.FullKey.DefaultPlugins).asScala

      for(pluginConfig:Config <- defaultPluginList){
        try {
          val name = pluginConfig.getString(ConfigConstant.PluginKey.Name)
          val typ = pluginConfig.getString(ConfigConstant.PluginKey.Type)
          val dependencies = pluginConfig.getStringList(ConfigConstant.PluginKey.Dependencies).asScala
          val changes = pluginConfig.getStringList(ConfigConstant.PluginKey.Changes).asScala
          val commands = pluginConfig.getStringList(ConfigConstant.PluginKey.Commands).asScala
          val pluginClass = pluginConfig.getString(ConfigConstant.PluginKey.ClassFile)
          val pluginEnvironment = None

          val pluginData = new PluginData(name,typ,dependencies.toArray,changes.toArray, commands.toArray, pluginClass, pluginEnvironment)

          mLoadedPlugins += ((pluginData.name, pluginData))

          Log.debug("plugin detected: " + name)
        } catch {
          case e:ConfigException =>
            Log.debug("plugin information missing!")
        }

      }
    }
  }

  def instantiateDefaultPlugin(className:String):ActorRef = {
    val pluginClass = Class.forName(className)
    context.system.actorOf(Props(pluginClass))
  }
}
