package io.glassdoor

import io.glassdoor.application.{Configuration, Constant, Context}
import io.glassdoor.interface.CommandLineInterface
import io.glassdoor.plugin.{DefaultPluginManager, Plugin}
import io.glassdoor.plugin.plugins.analyser.grep.GrepAnalyser
import io.glassdoor.plugin.plugins.loader.apk.ApkLoader
import io.glassdoor.plugin.plugins.preprocessor.extractor.Extractor
import io.glassdoor.plugin.plugins.preprocessor.smali.SmaliDisassembler

object Main {
  def main(args:Array[String]):Unit={

    Configuration.loadConfig()

    println("the first line of glassdoor!")

    var context = new Context
    context = Configuration.loadConfigIntoContext(context)

    val pluginManager = new DefaultPluginManager
    pluginManager.loadDefaultPlugins(context)

    //TODO: load default commands from config & command sequences

    pluginManager.applyPlugin("apk", Array("/home/flosch/glassdoor-testset/dvel.apk"), context)

    var result = pluginManager.getPluginResult("apk")

    //TODO: allow async callbacks here
    //only assign result if call was successful
    if(result.isDefined){
      context = result.get
    }

    pluginManager.applyPlugin("extractor",Array(Constant.Regex.REGEX_PATTERN_DEX, Constant.Context.FullKey.INTERMEDIATE_ASSEMBLY_DEX),context)
    result = pluginManager.getPluginResult("extractor")

    if(result.isDefined){
      context = result.get
    }

    pluginManager.applyPlugin("smali", Array(),context)
    result = pluginManager.getPluginResult("smali")

    if(result.isDefined){
      context = result.get
    }

    //pluginManager.applyPlugin("grep", Array(), context)

    //TODO: these plugins should be found dynamically
    //TODO: info about the plugins should be loaded via a manifest file (as main class)
//
//    val grep:Plugin = new GrepAnalyser
//
//    //TODO: the regex is still too broad
//    grep.apply(context,Array(Constant.REGEX_PATTERN_EMAIL,"intermediate-assembly.smali","result-log.grep-login"))
//    context = grep.result

  }
}
