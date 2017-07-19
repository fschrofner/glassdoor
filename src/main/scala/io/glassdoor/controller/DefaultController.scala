package io.glassdoor.controller

import java.io.File

import scala.collection.immutable.HashMap
import io.glassdoor.application._
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import io.glassdoor.plugin.PluginInstance
import io.glassdoor.resource.Resource

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 4/17/16.
  */
class DefaultController extends Controller{
  //a map of the aliases for commands
  var mAliasMap:Map[String,Array[String]] = new HashMap[String,Array[String]]

  override def handleChangedValues(changedValues:Map[String,String]){
    if(mContext.isDefined){
      //update context with values given in changed values
      for((key,value) <- changedValues){
        Log.debug("changing value for key: " + key)
        Log.debug("changed value: " + value)

        mContext.get.setResolvedValue(key,value)
      }

      forwardContextKeys(mContext.get.getDefinedKeys)
    }
  }


  override def handleContextUpdateRequestByPluginManager(): Unit = {
    sendContextUpdateToPluginManager()
  }

  override def handleRemovedValues(removedValues: Array[String]): Unit = {
    if(mContext.isDefined){
      //update context with removed values
      for(key <- removedValues){
        Log.debug("removing value with key: " + key)
        mContext.get.removeResolvedValue(key)
      }
    }
  }

  override def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit = {
    val commands = resolveAlias(pluginName, parameters)

    if(commands.length > 1){
      val names = commands.map(x => x.name)
      val parameters = commands.map(x => x.parameters)
      applyPlugins(names, parameters)
    } else if(commands.length == 1){
      applyPlugin(commands(0).name, commands(0).parameters)
    }
  }


  override def handleUiPrint(message: String): Unit = {
    forwardUiPrint(message)
  }


  override def handlePluginHelp(plugin: String): Unit = {
    forwardHelpForPlugin(plugin)
  }

  def resolveAlias(name:String, parameters:Array[String]):Array[Command] = {
    if(mAliasMap.contains(name)){
      val commandBuffer = ArrayBuffer[Command]()

      //load commands behind alias
      Log.debug("alias map contains command: " + name + ". replacing..")
      val commandStrings = mAliasMap.get(name).get

      for(commandString <- commandStrings){
        val command = CommandInterpreter.interpret(commandString)

        if(command.isDefined){
          //recursively call method in order to interpret other aliases
          commandBuffer ++= resolveAlias(command.get.name, command.get.parameters)
        } else {
          //TODO: could not interpret command!
          Log.debug("error: could not interpret command: " + name)
        }
      }
      return commandBuffer.toArray
    } else {
      return Array(Command(name, parameters))
    }
  }

  override def handlePluginTaskCompleted(pluginInstance: PluginInstance): Unit = {
    forwardTaskCompletedMessage(pluginInstance)
  }

  override def handleWaitForInput(): Unit = {
    forwardWaitForInput()
  }

  override def handleInstallResource(names:Array[String]):Unit = {
    for(name <- names){
      installResource(name)
    }
  }


  override def handleRemoveResource(names: Array[String]): Unit = {
    for(name <- names){
      removeResource(name)
    }
  }

  override def buildAliasIndex(context:Context):Unit = {
    val aliasConfigPath = context.getResolvedValue(ContextConstant.FullKey.ConfigAliasConfigPath)

    if(aliasConfigPath.isDefined){
      val file = new File(aliasConfigPath.get)
      val config = ConfigFactory.parseFile(file)

      val aliasList = config.getConfigList(ConfigConstant.ConfigKey.FullKey.Aliases).asScala

      for(aliasConfig:Config <- aliasList){
        try{
        //TODO: save aliases into hashmap
        val shorthand = aliasConfig.getString(ConfigConstant.AliasKey.Shorthand)
        val commands = aliasConfig.getStringList(ConfigConstant.AliasKey.Commands).asScala
          mAliasMap += ((shorthand,commands.toArray))

          Log.debug("alias detected: "  + shorthand)
        } catch {
          case e:ConfigException =>
            Log.debug("alias information missing")
        }

      }
    }

    //send to ui
    forwardAliasList(mAliasMap.keys.toArray)
  }

  override def handlePluginError(pluginInstance: Option[PluginInstance], errorCode: Integer, data: Option[Any]): Unit = {
    forwardPluginErrorMessage(pluginInstance,errorCode,data)
  }


  override def handleResourceError(resource: Option[Resource], errorCode: Integer, data: Option[Any]): Unit = {
    forwardResourceErrorMessage(resource, errorCode, data)
  }


  override def handleResourceSuccess(resource: Option[Resource], code: Integer): Unit = {
    forwardResourceSuccessMessage(resource, code)
  }

  override def handleUpdateAvailableResources(): Unit = {
    updateAvailableResources()
  }

  override def handlePluginList(): Unit = {
    forwardPluginList()
  }

  //  def launchPluginTest(): Unit ={
  //    val pluginManager = new DefaultPluginManager
  //    pluginManager.loadDefaultPlugins(context)
  //
  //    //TODO: load default commands from config & command sequences
  //
  //    pluginManager.applyPlugin("apk", Array("/home/flosch/glassdoor-testset/dvel.apk"), context)
  //
  //    var result = pluginManager.getPluginResult("apk")
  //
  //    //TODO: allow async callbacks here
  //    //only assign result if call was successful
  //    if(result.isDefined){
  //      context = result.get
  //    }
  //
  //    pluginManager.applyPlugin("extractor",Array(Constant.Regex.REGEX_PATTERN_DEX, ContextConstant.FullKey.INTERMEDIATE_ASSEMBLY_DEX),context)
  //    result = pluginManager.getPluginResult("extractor")
  //
  //    if(result.isDefined){
  //      context = result.get
  //    } else {
  //      println("there was an error: context of extractor not saved!")
  //    }
  //
  //    pluginManager.applyPlugin("smali", Array(),context)
  //    result = pluginManager.getPluginResult("smali")
  //
  //    if(result.isDefined){
  //      context = result.get
  //    }
  //
  //    //TODO: the regex is still too broad
  //    pluginManager.applyPlugin("grep", Array(Constant.Regex.REGEX_PATTERN_EMAIL,ContextConstant.FullKey.INTERMEDIATE_ASSEMBLY_SMALI, ContextConstant.FullKey.RESULT_LOG_GREP_LOGIN), context)
  //
  //    result = pluginManager.getPluginResult("grep")
  //
  //    if(result.isDefined){
  //      context = result.get
  //    }
  //
  //    //TODO: create plugins that update resources and plugins, using the urls from the config file
  //    //pluginManager.applyPlugin("git", Array(Configuration.getString(ConfigConstant.ConfigKey.FullKey.RESOURCE_REPOSITORY), "dictionaries/seclists"), context)
  //  }
}
