package io.glassdoor.controller

import java.io.File

import scala.collection.immutable.HashMap
import io.glassdoor.application._
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import scala.collection.JavaConverters._

/**
  * Created by Florian Schrofner on 4/17/16.
  */
class DefaultController extends Controller{
  //a map of the aliases for commands
  var mAliasMap:Map[String,Array[String]] = new HashMap[String,Array[String]]

  override def handleChangedValues(changedValues:Map[String,String]){
    //update context with values given in changed values
    for((key,value) <- changedValues){
      Log.debug("changing value for key: " + key)
      Log.debug("changed value: " + value)

      mContext.setResolvedValue(key,value)
    }
  }

  override def handleApplyPlugin(pluginName:String, parameters:Array[String]):Unit = {

    if(mAliasMap.contains(pluginName)){
      //load commands behind alias and execute them
      Log.debug("alias map contains command! replacing..")
      val commandStrings = mAliasMap.get(pluginName).get

      for(commandString <- commandStrings){
        val command = CommandInterpreter.interpret(commandString)

        if(command.isDefined){
          applyPlugin(command.get.name,command.get.parameters)
        } else {
          //TODO: could not interpret command!
        }

      }
    } else {
      //directly launch plugin
      applyPlugin(pluginName, parameters)
    }
  }

  override def handleInstallResource(names:Array[String]):Unit = {
    for(name <- names){
      installResource(name)
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
  }

  override def handlePluginError(pluginId: Long, errorCode: Integer, data: Option[Any]): Unit = {
    forwardErrorMessage(pluginId,errorCode,data)
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
