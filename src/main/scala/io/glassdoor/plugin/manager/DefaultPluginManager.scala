package io.glassdoor.plugin.manager

import java.io.File

import akka.actor.{ActorRef, Props}
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import io.glassdoor.application._
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.manager.ChangeStatus.ChangeStatus
import io.glassdoor.plugin.manager.DependencyStatus.DependencyStatus
import io.glassdoor.plugin.manager.PluginManagerConstant.PluginErrorCode
import io.glassdoor.plugin._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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
  var mPluginQueue:ArrayBuffer[ScheduledPlugin] = ArrayBuffer[ScheduledPlugin]()

  //a map of all the loaded plugins, plugin names are the key values
  var mLoadedPlugins:Map[String, PluginData] = new scala.collection.immutable.HashMap[String,PluginData]

  override def loadPlugin(pluginName: String): Unit = ???

  override def unloadPlugin(pluginName: String): Unit = ???

  override def findPlugin(pluginName: String): Array[String] = {
    val pluginList = mLoadedPlugins.valuesIterator.toList
    val nameList = pluginList.map(x => x.name)

    if(pluginName == Constant.Parameter.Any){
      return nameList.toArray
    } else {
      //TODO: find also partially matching plugin names
      if(nameList.contains(pluginName)){
        return Array(pluginName)
      } else {
        Array.empty[String]
      }
    }
  }


  override def showHelpForPlugin(pluginName: String): Unit = {
    if(pluginName == null || pluginName.isEmpty){
      printInUserInterface("error: no plugin specified!")
    } else {
      val plugin = mLoadedPlugins.get(pluginName)
      if(plugin.isDefined){
        printInUserInterface(plugin.get.help)
      } else {
        printInUserInterface("error: plugin not found!")
      }
    }

    readyForNewInput()
  }

  def sendPluginsForAutocomplete():Unit = {
    val buffer = ArrayBuffer[String]()

    for(plugin <- mLoadedPlugins.values){
      buffer.append(plugin.name)

      if(plugin.commands != null && plugin.commands.length > 0){
        for(command <- plugin.commands){
          buffer.append(plugin.name + " " + command)
        }
      }
    }

    EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.PluginCommandList, Some(buffer.toArray))))
  }

  override def handlePluginFailure(pluginId: Long, errorMessage: Option[String]): Unit = {
    Log.debug("plugin failed, handling error..")
    val matchingPlugin = getRunningPluginInstance(pluginId)

    if(matchingPlugin.isDefined){
      clearChangingValuesAndDependencies(matchingPlugin.get)
    }

    mRunningPlugins.remove(pluginId)

    if(errorMessage.isDefined){
      Log.debug("error message is defined, printing error in user interface..")
      printInUserInterface(errorMessage.get)
    }

    startNextPlugin()
  }

  def getRunningPluginInstance(pluginId: Long) : Option[PluginInstance] = {
    var matchingPlugin:Option[PluginInstance] = None

    if(mRunningPlugins.contains(pluginId)){
      matchingPlugin = mRunningPlugins.get(pluginId)
    }

    matchingPlugin
  }

  def startNextPlugin() : Unit = {
    //start next (launchable) plugin in queue
    if(mPluginQueue.nonEmpty){
      Log.debug("plugin queue not empty! requesting context update")
      requestContextUpdate()
    } else {
      Log.debug("plugin queue empty, not requesting context update")
      if(mRunningPlugins.isEmpty){
        Log.debug("also no running plugins: ready for new input")
        readyForNewInput()
      } else {
        Log.debug("but still running plugins, not ready for new input")
      }
    }
  }

  override def handlePluginResult(pluginId:Long, changedValues:Map[String,String]):Unit = {
    //TODO: check if permissions are met

    Log.debug("plugin manager received plugin result!")

    val matchingPlugin = getRunningPluginInstance(pluginId)

    if(matchingPlugin.isDefined){
      val pluginInstance = matchingPlugin.get

      Log.debug("received result from: " + pluginInstance.name)

      for((key,value) <- changedValues){
        if(pluginInstance.changes.contains(key)){
          Log.debug("correctly changing key: " + key)
        } else {
          Log.debug("error: change not specified in manifest! " + key)
          return
        }
      }

      applyChangedValues(changedValues)

      clearChangingValuesAndDependencies(pluginInstance)
      mRunningPlugins.remove(pluginId)

      startNextPlugin()

    } else {
      Log.debug("no matching plugin found!")
    }
  }

  def clearChangingValuesAndDependencies(pluginInstance:PluginInstance):Unit = {
    Log.debug("clear changing values and dependencies called!")
    val changes = pluginInstance.changes
    val dependencies = pluginInstance.dependencies

    Log.debug("length of values in change: " + mChangingValues.length)
    Log.debug("length of changes stopped: " + changes.length)

    for(change <- changes){
      mChangingValues = mChangingValues.filterNot(_ == change)
    }

    Log.debug("length of values in change afterwards: " + mChangingValues.length)

    for(dependency <- dependencies){
      val prevVal = mWorkedOnDependencies.get(dependency).get
      if(prevVal > 1){
        mWorkedOnDependencies.put(dependency, prevVal - 1)
      } else {
        mWorkedOnDependencies.remove(dependency)
      }
    }
  }


  override def handleContextUpdate(context: Context): Unit = {
    Log.debug("received context update, executing next plugin from queue")
    executeNextFromPluginQueue(context)
  }

  /*
   * Executes the next launchable plugins from the plugin queue.
   * This method should be called after another plugin completed or dynamic plugins dependencies/changes were resolved.
   */
  def executeNextFromPluginQueue(context:Context):Unit = {
    if(!mPluginQueue.isEmpty){
      var allDependenciesUnsatisfied = true

      for(plugin <- mPluginQueue){
        val dependencyResult = checkAndGetDependencies(plugin.dependencies,context)
        val changeResult = checkAndGetChangedValues(plugin.changes, context)

        //if there is at least one plugin that has not unsatisfied
        //dependencies for sure, there might be a way..
        if(dependencyResult.status != DependencyStatus.Unsatisfied){
          allDependenciesUnsatisfied = false
        }

        if(dependencyResult.status == DependencyStatus.Satisfied){
          Log.debug("dependencies satisfied!")
        } else {
          Log.debug("dependencies unsatisfied")
        }

        if(changeResult.status == ChangeStatus.Satisfied){
          Log.debug("changes satisfied")
        } else {
          Log.debug("changes unsatisfied")
        }

        if(dependencyResult.status == DependencyStatus.Satisfied && changeResult.status == ChangeStatus.Satisfied){
          //plugin is now ready to launch
          if(dependencyResult.data.isDefined && changeResult.data.isDefined){
            val valueHashMap = new scala.collection.mutable.HashMap[String,String]

            valueHashMap ++= dependencyResult.data.get.asInstanceOf[Map[String,String]]
            valueHashMap ++= changeResult.data.get.asInstanceOf[Map[String,String]]
            valueHashMap ++= getConfigValues(context)

            mPluginQueue = mPluginQueue.filterNot(_ == plugin)

            applyPlugin(plugin.asPluginData(), valueHashMap.toMap, plugin.dependencies, plugin.changes, plugin.parameters)
          }
        } else if(dependencyResult.status == DependencyStatus.Unsatisfied && mRunningPlugins.isEmpty && mPluginQueue.length == 1){
          //dependency will not become available, as the plugin itself is the only scheduled plugin
          mPluginQueue = mPluginQueue.filterNot(_ == plugin)
          if(dependencyResult.data.isDefined){
            sendErrorMessage(None, PluginManagerConstant.PluginErrorCode.DependenciesNotSatisfied,Some(dependencyResult.data.get.asInstanceOf[String]))
          }
          readyForNewInput()
        }
      }

      if(allDependenciesUnsatisfied && mRunningPlugins.isEmpty){
        mPluginQueue.clear()
        printInUserInterface("error: some dependencies were not satisfied and there are no running plugins that can provide them")
        readyForNewInput()
      }

    } else {
      Log.debug("plugin queue is empty!")
      readyForNewInput()
    }
  }



  /**
    * Checks if the dependencies are met and only if all dependencies are met and not in use (= dependency status Satisfied), they get returned as Map[String,String].
    * The dependency status can be either Satisfied, Unsatisfied, InUse or Dynamic.
    * If the status Unsatisfied or InUse is returned, the data contains the descriptor string of the dependency causing that status.
 *
    * @param dependencies a list of dependencies as context descriptors
    * @param context the current context
    * @return the result containing a status and data (if status = Satisfied)
    */
  def checkAndGetDependencies(dependencies:Array[String], context:Context):DependencyResult = {
    val mutableHashmap = new scala.collection.mutable.HashMap[String,String]
    Log.debug("checking and getting the dependencies..")

    if(dependencies.length < 1){
      Log.debug("dependencies less than 1! automatically satisfied")
      return DependencyResult(DependencyStatus.Satisfied, Some(mutableHashmap.toMap))
    } else {
      Log.debug("dependencies size: " + dependencies.length + " checking..")
    }

    //save first error result, when an error is encountered
    var errorResult:Option[DependencyResult] = None;

    //provide access to the dependencies and add them to the current dependencies
    for(dependency <- dependencies){

      Log.debug("checking dependency: " + dependency)

      //dynamic dependencies need to be resolved first
      if(dependency == PluginManagerConstant.DynamicDependency){
        Log.debug("dynamic dependency, needs to be resolved first")
        //instantly return here, dependency needs to be resolved!
        return DependencyResult(DependencyStatus.Dynamic, None)
      }

      val value = context.getResolvedValue(dependency)

      if(value.isDefined) {
        if (mChangingValues.contains(dependency)) {
          Log.debug("dependency in change! can not safely launch plugin!")
          if(errorResult.isEmpty) errorResult = Some(DependencyResult(DependencyStatus.InUse, None))
        } else {
          Log.debug("dependency: " + dependency + " satisfied")
          mutableHashmap.put(dependency, value.get)
        }
      } else {
        //there might be multiple dependencies, that are not satisfied, but it only saves the first mismatch
        Log.debug("dependency: " + dependency + " not satisfied!")
        if(errorResult.isEmpty) errorResult = Some(DependencyResult(DependencyStatus.Unsatisfied, Some(dependency)))
      }
    }

    //if there has been an error
    if(errorResult.isDefined){
      return errorResult.get;
    } else {
      DependencyResult(DependencyStatus.Satisfied, Some(mutableHashmap.toMap))
    }
  }

  def checkAndGetChangedValues(changes:Array[String], context:Context):ChangeResult = {
    Log.debug("check and get changed values called, size of changes: " + changes.length)

    val mutableHashmap = new scala.collection.mutable.HashMap[String,String]

    //provide access to values it changes and add value to changed values
    for(change <- changes){

      //dynamic changes need to be resolved first
      if(change == PluginManagerConstant.DynamicDependency){
        Log.debug("change dynamic, needs to be resolved first!")
        return ChangeResult(ChangeStatus.Dynamic, None)
      }

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

    Log.debug("change status satisfied!")
    ChangeResult(ChangeStatus.Satisfied, Some(mutableHashmap.toMap))
  }

  def getConfigValues(context:Context):Map[String,String] = {
    val configHashmap = new scala.collection.mutable.HashMap[String,String]

    //provide configuration in context
    val configKeymapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.Config)

    if(configKeymapOpt.isDefined){
      val configKeymap = configKeymapOpt.get

      for((key,value) <- configKeymap){
        val fullKey = ContextConstant.Keymap.Config + ContextConstant.DescriptorSplit + key
        configHashmap.put(fullKey,value)
      }
    }

    configHashmap.toMap
  }
  def updateDependencyCounts(dependencies:Array[String]):Unit = {
    for(dependency <- dependencies){
      Log.debug("dependency added: " + dependency)
      if (mWorkedOnDependencies.contains(dependency)) {
        val prevVal = mWorkedOnDependencies.get(dependency).get
        mWorkedOnDependencies.put(dependency, prevVal + 1)
      } else {
        mWorkedOnDependencies.put(dependency, 1)
      }
    }
  }

  def updateChangeCounts(changes:Array[String]):Unit = {
    Log.debug("values in change: " + changes.length)

    for(change <- changes){
      Log.debug("in change: " + change)
      mChangingValues.append(change)
    }
  }

  /**
    * Directly launches the plugin using the specified values. Please note that this needs the RESOLVED values, so the data
    * given is directly handed to the plugin. Dependencies and changes only need to be a list of the key descriptors, these are used to update
    * the stats.
    */
  private def applyPlugin(pluginData:PluginData, values:Map[String,String], dependencies:Array[String], changes:Array[String], parameters:Array[String]): Unit ={

    val actor = instantiatePlugin(pluginData.pluginClass, pluginData.pluginEnvironment)

    //if plugin instantiation successful
    if(actor.isDefined){
      //mark dependencies and changes as "in use"
      Log.debug("size of dependencies, when applying: "+ dependencies.size)
      Log.debug("size of changes, when applying: " + changes.size)

      updateDependencyCounts(dependencies)
      updateChangeCounts(changes)

      val id = UniqueIdGenerator.generate()

      Log.debug("starting plugin " + pluginData.name + " with id: " + id)

      actor.get ! Message(PluginConstant.Action.SetUniqueId, Some(id))

      //create a new plugin instance with the data
      val pluginInstance = PluginInstance(id, pluginData.name, pluginData.kind, pluginData.dependencies, pluginData.changes, pluginData.commands, actor.get)

      mRunningPlugins.put(pluginInstance.uniqueId, pluginInstance)

      //apply plugin
      actor.get ! Message(PluginConstant.Action.Apply, Some(new PluginParameters(values, parameters)))
    }
  }

  /**
    * Launches the plugin stored inside the given plugin data object.
    */
  def applyPlugin(pluginData:PluginData, parameters: Array[String], context: Context): Unit = {
    Log.debug("plugin manager: apply plugin by data called! name: " + pluginData.name)

    val valueHashMap = new scala.collection.mutable.HashMap[String,String]

    val dependencies = pluginData.dependencies
    val changes = pluginData.changes

    //check and provide the values the plugin needs
    val dependencyResult = checkAndGetDependencies(dependencies,context)

    dependencyResult.status match {
      case DependencyStatus.Satisfied =>
        if(dependencyResult.data.isDefined){
          valueHashMap ++= dependencyResult.data.get.asInstanceOf[Map[String,String]]
        }
      case DependencyStatus.Unsatisfied =>
        if(dependencyResult.data.isDefined){
          val dependency = dependencyResult.data.get.asInstanceOf[String]

          Log.debug("dependency unsatisfied, checking plugin queue..")

          //send error when there are no other scheduled plugins to change the value
          if(mPluginQueue.isEmpty){
            Log.debug("plugin queue empty! error!")
            sendErrorMessage(None, PluginManagerConstant.PluginErrorCode.DependenciesNotSatisfied,Some(dependency))
            readyForNewInput()
          } else {
            Log.debug("plugin queue not empty. adding plugin to queue")
            mPluginQueue.append(ScheduledPlugin(pluginData, parameters))
          }
          return
        }
      case DependencyStatus.InUse =>
        if(dependencyResult.data.isDefined){
          val dependency = dependencyResult.data.get.asInstanceOf[String]
          //TODO: why send an error message here?
          sendErrorMessage(None, PluginManagerConstant.PluginErrorCode.DependenciesInChange, Some(dependency))
          mPluginQueue.append(ScheduledPlugin(pluginData, parameters))
          return
        }
      case DependencyStatus.Dynamic =>
        Log.debug("dependency is dynamic, need to wait for resolved dependency!")
        resolveDynamicValues(pluginData, parameters)
        return
    }

    //check and provide the values the plugin works on
    val changeResult = checkAndGetChangedValues(changes, context)

    changeResult.status match {
      case ChangeStatus.Satisfied =>
        if(changeResult.data.isDefined){
          valueHashMap ++= changeResult.data.get.asInstanceOf[Map[String,String]]
        }
      case ChangeStatus.InUse =>
        mPluginQueue.append(ScheduledPlugin(pluginData, parameters))
        return
      case ChangeStatus.Dynamic =>
        Log.debug("change is dynamic, need to wait for resolved change!")
        resolveDynamicValues(pluginData, parameters)
        return
    }

    valueHashMap ++= getConfigValues(context)

    applyPlugin(pluginData, valueHashMap.toMap, dependencies, changes, parameters)
  }

  def resolveDynamicValues(pluginData:PluginData, parameters:Array[String]):Unit = {
    val id = UniqueIdGenerator.generate()
    val actor = instantiatePlugin(pluginData.pluginClass, pluginData.pluginEnvironment)

    Log.debug("queued dynamic plugin " + pluginData.name + " with id: " + id)
    actor.get ! Message(PluginConstant.Action.SetUniqueId, Some(id))

    mPluginQueue.append(ScheduledPlugin(Some(id), pluginData, parameters))
    actor.get ! Message(PluginConstant.Action.ResolveDynamicValues, Some(parameters))

    //TODO: the actor should probably get saved in the scheduled plugin so that it does not instantiate a new one
  }


  override def handleResolvedDynamicValues(dynamicValues: DynamicValues): Unit = {
    Log.debug("handle resolved dynamic values")
    if(dynamicValues.dependencies.isDefined)Log.debug("resolved dependencies")
    if(dynamicValues.changes.isDefined)Log.debug("resolved changes")

    for(plugin <- mPluginQueue){
      if(plugin.id.isDefined && dynamicValues.uniqueId.isDefined && plugin.id.get == dynamicValues.uniqueId.get){
        if(dynamicValues.dependencies.isDefined){
            //delete the dynamic dependencies
            plugin.dependencies = plugin.dependencies.filterNot(_ == PluginManagerConstant.DynamicDependency)
            //append the resolved dependencies
            plugin.dependencies ++= dynamicValues.dependencies.get
        } else {
          if(plugin.dependencies.contains(PluginManagerConstant.DynamicDependency)){
            //TODO: send error message instead of print in user interface?
            printInUserInterface("error: the plugin can not resolve its dependencies with the given call")
            mPluginQueue = mPluginQueue.filterNot(_ == plugin)
          }
        }

        if(dynamicValues.changes.isDefined){
          //delete dynamic changes
          plugin.changes = plugin.changes.filterNot(_ == PluginManagerConstant.DynamicDependency)
          //append the resolved changes
          plugin.changes ++= dynamicValues.changes.get
        } else {
          if(plugin.dependencies.contains(PluginManagerConstant.DynamicDependency)){
            //TODO: send error message instead of print in user interface?
            printInUserInterface("error: the plugin can not resolve its changes with the given call")
            mPluginQueue = mPluginQueue.filterNot(_ == plugin)
          }
        }
      }
    }

    requestContextUpdate()
  }

  /**
    * Apply plugin with the given name. This is normally directly called from the user.
    * Fetches the plugin with the name from the loaded plugins and then launches it.
    */
  override def applyPlugin(pluginName: String, parameters: Array[String], context: Context): Unit = {
    Log.debug("plugin manager: apply plugin by name called! name: " + pluginName)

    val pluginDataOpt = getPluginDataForName(pluginName)

    if(pluginDataOpt.isDefined){
      applyPlugin(pluginDataOpt.get, parameters, context)
    } else {
      Log.debug("error: plugin not found!")
      sendErrorMessage(None, PluginErrorCode.PluginNotFound, None)
      readyForNewInput()
    }
  }

  def getPluginDataForName(pluginName:String):Option[PluginData] = {
    var pluginDataOpt:Option[PluginData] = None

    if(mLoadedPlugins.contains(pluginName)){
      pluginDataOpt = mLoadedPlugins.get(pluginName)
    } else {
      pluginDataOpt = None
    }

    pluginDataOpt
  }

  def instantiatePlugin(pluginClass:String, pluginEnvironment:Option[Map[String, String]] = None):Option[ActorRef] = {
    val targetClass = Class.forName(pluginClass)
    val actor = context.system.actorOf(Props(targetClass))

    if(pluginEnvironment.isDefined){
      //TODO: send environment to plugin, e.g. path to groovy script
      actor ! Message(PluginConstant.Action.SetUniqueId, pluginEnvironment)

    }

    Some(actor)
  }

  override def applyPlugins(pluginNames: Array[String], parameters: Array[Array[String]], context: Context): Unit = {
    //check if all plugins have parameters provided
    if(pluginNames.length == parameters.length){
      //generate plugin data
      for(i <- pluginNames.indices){
        val pluginDataOpt = getPluginDataForName(pluginNames(i))

        if(pluginDataOpt.isDefined){
          val dependencyResult = checkAndGetDependencies(pluginDataOpt.get.dependencies, context)
          val changeResult = checkAndGetChangedValues(pluginDataOpt.get.changes, context)

          if(dependencyResult.status == DependencyStatus.Dynamic || changeResult.status == ChangeStatus.Dynamic){
            resolveDynamicValues(pluginDataOpt.get, parameters(i))
          } else {
            mPluginQueue.append(ScheduledPlugin(pluginDataOpt.get,parameters(i)))
          }
        }
      }

      executeNextFromPluginQueue(context)
    } else {
      Log.debug("error: some plugins are missing parameters!")
    }
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
          val help = pluginConfig.getString(ConfigConstant.PluginKey.Help)

          val pluginData = new PluginData(name,typ,dependencies.toArray,changes.toArray, commands.toArray, pluginClass, pluginEnvironment, help)

          mLoadedPlugins += ((pluginData.name, pluginData))

          Log.debug("plugin detected: " + name)
        } catch {
          case e:ConfigException =>
            Log.debug("plugin information missing!")
        }

      }

      sendPluginsForAutocomplete()
    }
  }

  def instantiateDefaultPlugin(className:String):ActorRef = {
    val pluginClass = Class.forName(className)
    context.system.actorOf(Props(pluginClass))
  }

  def loadInstalledPlugins(context:Context): Unit = {
    val pluginDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigPluginDirectory)

    //TODO: iterate over plugin directory, read config files and add plugins
    if(pluginDirOpt.isDefined){
      val pluginDirectory = new File(pluginDirOpt.get)
      val subFiles = pluginDirectory.list()

      if(subFiles != null){
        for(subFile <- subFiles){
          //TODO: read conf file and load plugin
          val pluginFolder = new File(subFile)
          if(pluginFolder.isDirectory && pluginFolder.list().length > 0){
            for(fileName <- pluginFolder.list()){
              val extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())

              if(extension == PluginManagerConstant.PluginConfFileExtension){
                val pluginData = parseLocalPlugin(fileName)
                mLoadedPlugins += ((pluginData.name, pluginData))
              }
            }
          }
        }
      }
    }
  }

  def parseLocalPlugin(confFilePath:String):PluginData = {
    val confFile = new File(confFilePath)
    val pluginConfig = ConfigFactory.parseFile(confFile)

    val name = pluginConfig.getString(ConfigConstant.PluginKey.Name)
    val typ = pluginConfig.getString(ConfigConstant.PluginKey.Type)
    val dependencies = pluginConfig.getStringList(ConfigConstant.PluginKey.Dependencies).asScala
    val changes = pluginConfig.getStringList(ConfigConstant.PluginKey.Changes).asScala
    val commands = pluginConfig.getStringList(ConfigConstant.PluginKey.Commands).asScala
    val interpreter = pluginConfig.getString(ConfigConstant.PluginKey.Interpreter)
    var pluginClass:Option[String] = None
    val pluginEnvironment = new mutable.HashMap[String,String]()
    val help = pluginConfig.getString(ConfigConstant.PluginKey.Help)

    interpreter match {
      case PluginEnvironmentConstant.Interpreter.Groovy =>
        pluginClass = Some("io.glassdoor.plugin.language.GroovyPlugin")
        //TODO: dynamically insert all values inside enviroment object
        pluginEnvironment.put(PluginEnvironmentConstant.Key.MainClass, pluginConfig.getString(PluginEnvironmentConstant.Key.MainClass))
      case _ =>
        Log.debug("error: unknown plugin interpreter")
    }

    val pluginData = new PluginData(name,typ,dependencies.toArray,changes.toArray, commands.toArray, pluginClass.get, if(pluginEnvironment.nonEmpty)Some(pluginEnvironment.toMap) else None, help)

    null
  }

  override def getPluginInstance(pluginId: Long): Option[PluginInstance] = {
    mRunningPlugins.get(pluginId)
  }
}

/**
  * This is used to store values in the plugin queue. When handing over plugin data and parameters,
  * each value is cloned so that they can be changed independently of the original values (especially important
  * for dynamic dependencies and changes).
  */
case class ScheduledPlugin(id:Option[Long], name:String, kind:String, var dependencies:Array[String], var changes:Array[String],
                           commands:Array[String], pluginClass:String, pluginEnvironment:Option[Map[String,String]], parameters:Array[String], help:String){

  def asPluginData():PluginData = {
    return PluginData(name, kind, dependencies, changes, commands, pluginClass, pluginEnvironment, help)
  }
}

object ScheduledPlugin{
  def apply(pluginData:PluginData, parameters:Array[String]):ScheduledPlugin = {
    ScheduledPlugin(None, pluginData, parameters)
  }

  def apply(id:Option[Long], pluginData:PluginData, parameters:Array[String]):ScheduledPlugin = {
    val name = pluginData.name
    val kind = pluginData.kind
    val dependencies = pluginData.dependencies.clone()
    val changes = pluginData.changes.clone()
    val commands = pluginData.commands.clone()
    val pluginClass = pluginData.pluginClass
    val pluginEnvironment = pluginData.pluginEnvironment
    val help = pluginData.help

    new ScheduledPlugin(id, name, kind, dependencies, changes, commands, pluginClass, pluginEnvironment, parameters, help)
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
  val Dynamic, Satisfied, InUse = Value
}