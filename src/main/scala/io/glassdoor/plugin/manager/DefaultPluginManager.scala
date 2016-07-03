package io.glassdoor.plugin.manager

import java.io.File

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import groovy.lang.GroovyClassLoader
import io.glassdoor.application._
import io.glassdoor.bus.Message
import io.glassdoor.plugin.language.GroovyPlugin
import io.glassdoor.plugin.manager.ChangeStatus.ChangeStatus
import io.glassdoor.plugin.manager.DependencyStatus.DependencyStatus
import io.glassdoor.plugin.manager.PluginManagerConstant.PluginErrorCode
import io.glassdoor.plugin.{Plugin, PluginConstant, PluginData, PluginInstance, PluginParameters, PluginResult}

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

    Log.debug("plugin manager received plugin result!")

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
          Log.debug("error: change not specified in manifest! " + key)
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

  def executeNextFromPluginQueue():Unit = {
    if(!mPluginQueue.isEmpty){
      val pluginArray = mPluginQueue.toArray

      for(plugin <- pluginArray){

      }
    }
  }

  /**
    * Checks if the dependencies are met and only if all dependencies are met and not in use (= dependency status Satisfied), they get returned as Map[String,String].
    * The dependency status can be either Satisfied, Unsatisfied, InUse or Dynamic.
    * If the status Unsatisfied or InUse is returned, the data contains the descriptor string of the dependency causing that status.
    * @param dependencies a list of dependencies as context descriptors
    * @param context the current context
    * @return the result containing a status and data (if status = Satisfied)
    */
  def checkAndGetDependencies(dependencies:Array[String], context:Context):DependencyResult = {
    val mutableHashmap = new scala.collection.mutable.HashMap[String,String]

    //provide access to the dependencies and add them to the current dependencies
    for(dependency <- dependencies){

      //dynamic dependencies need to be resolved first
      if(dependency == PluginManagerConstant.DynamicDependency){
        return DependencyResult(DependencyStatus.Dynamic, None)
      }

      val value = context.getResolvedValue(dependency)

      if(value.isDefined) {
        if (mChangingValues.contains(dependency)) {
          Log.debug("dependency in change! can not safely launch plugin!")
          return DependencyResult(DependencyStatus.InUse, None)
        } else {
          mutableHashmap.put(dependency, value.get)
        }
      } else {
        //there might be multiple dependencies, that are not satisfied, but it already stops at the first mismatch
        Log.debug("dependency: " + dependency + " not satisfied!")
        return DependencyResult(DependencyStatus.Unsatisfied, Some(dependency))
      }
    }

    DependencyResult(DependencyStatus.Satisfied, Some(mutableHashmap.toMap))
  }

  def checkAndGetChangedValues(changes:Array[String], context:Context):ChangeResult = {
    val mutableHashmap = new scala.collection.mutable.HashMap[String,String]

    //provide access to values it changes and add value to changed values
    for(change <- changes){
      if(mWorkedOnDependencies.contains(change) || mChangingValues.contains(change)){
        Log.debug("value is depended on or already changing! can not safely launch plugin!")
        return ChangeResult(ChangeStatus.InUse, None)
      } else {
        val value = context.getResolvedValue(change)
        if(value.isDefined){
          mutableHashmap.put(change, value.get)
        }
      }
    }

    ChangeResult(ChangeStatus.Satisfied, Some(mutableHashmap.toMap))
  }

  def updateDependencyCounts(dependencies:Array[String]):Unit = {
    for(dependency <- dependencies){
      if (mWorkedOnDependencies.contains(dependency)) {
        val prevVal = mWorkedOnDependencies.get(dependency).get
        mWorkedOnDependencies.put(dependency, prevVal + 1)
      } else {
        mWorkedOnDependencies.put(dependency, 1)
      }
    }
  }

  def updateChangeCounts(changes:Array[String]):Unit = {
    for(change <- changes){
      mChangingValues.append(change)
    }
  }

  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    Log.debug("plugin manager: apply plugin called!")

    val mutableHashmap = new scala.collection.mutable.HashMap[String,String]

    var pluginDataOpt:Option[PluginData] = None

    if(mLoadedPlugins.contains(pluginName)){
      pluginDataOpt = mLoadedPlugins.get(pluginName)
    } else {
      Log.debug("error: plugin not found!")
      sendErrorMessage(None, PluginErrorCode.PluginNotFound, None)
    }

    if(pluginDataOpt.isDefined){
      val pluginData = pluginDataOpt.get
      val dependencies = pluginData.dependencies
      val changes = pluginData.changes

      //check and provide the values the plugin needs
      val dependencyResult = checkAndGetDependencies(dependencies,context)

      dependencyResult.status match {
        case DependencyStatus.Satisfied =>
          if(dependencyResult.data.isDefined){
            mutableHashmap ++= dependencyResult.data.get.asInstanceOf[Map[String,String]]
          }
        case DependencyStatus.Unsatisfied =>
          if(dependencyResult.data.isDefined){
            val dependency = dependencyResult.data.get.asInstanceOf[String]
            sendErrorMessage(None, PluginManagerConstant.PluginErrorCode.DependenciesNotSatisfied,Some(dependency))
            return
          }
        case DependencyStatus.InUse =>
          if(dependencyResult.data.isDefined){
            val dependency = dependencyResult.data.get.asInstanceOf[String]
            sendErrorMessage(None, PluginManagerConstant.PluginErrorCode.DependenciesInChange, Some(dependency))
            //TODO: add plugin to wait queue
            return
          }
        case DependencyStatus.Dynamic =>
          //TODO: start resolution of dynamic dependency here/add plugin to wait queue
      }


      //check and provide the values the plugin works on
      val changeResult = checkAndGetChangedValues(changes, context)

      changeResult.status match {
        case ChangeStatus.Satisfied =>
          if(changeResult.data.isDefined){
            mutableHashmap ++= changeResult.data.get.asInstanceOf[Map[String,String]]
          }
        case ChangeStatus.InUse =>
          //TODO: add plugin to wait queue
          return
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
        //mark dependencies and changes as "in use"
        updateDependencyCounts(dependencies)
        updateChangeCounts(changes)

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

  override def getPluginInstance(pluginId: Long): Option[PluginInstance] = {
    mRunningPlugins.get(pluginId)
  }
}

case class DependencyResult(status: DependencyStatus, data: Option[Any])

object DependencyStatus extends Enumeration {
  type DependencyStatus = Value
  val  Dynamic, Unsatisfied, Satisfied, InUse = Value
}

case class ChangeResult (status:ChangeStatus, data: Option[Any])

object ChangeStatus extends Enumeration {
  type ChangeStatus = Value
  val Satisfied, InUse = Value
}