package io.glassdoor.controller

/**
  * Created by Florian Schrofner on 4/17/16.
  */
class DefaultController extends Controller{
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
