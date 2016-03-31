package io.glassdoor

import io.glassdoor.application.{Configuration, Constant, Context}
import io.glassdoor.interface.CommandLineInterface
import io.glassdoor.plugin.Plugin
import io.glassdoor.plugin.plugins.analyser.grep.GrepAnalyser
import io.glassdoor.plugin.plugins.loader.apk.ApkLoader
import io.glassdoor.plugin.plugins.preprocessor.extractor.Extractor
import io.glassdoor.plugin.plugins.preprocessor.smali.SmaliDisassembler

object Main {
  def main(args:Array[String]):Unit={

    Configuration.loadConfig()

    println("the first line of glassdoor!")

    val foobar = Configuration.getString("foo.bar")

    if(foobar.isDefined){
      println(foobar.get)
    } else {
      println("ERROR: foobar not defined")
    }

    var context = new Context

    //val ui = new CommandLineInterface()
    //ui.test()

    //TODO: these plugins should be found dynamically
    //TODO: info about the plugins should be loaded via a manifest file (as main class)
    val apkLoader:Plugin = new ApkLoader
    //TODO: these calls should be done by the pluginmanager
    apkLoader.apply(context, Array("/home/flosch/glassdoor-testset/dvel.apk"))
    //TODO: allow async callbacks here
    context = apkLoader.result
    println("loaded apk: " + context.originalBinary(Constant.ORIGINAL_BINARY_APK))

    println("trying to extract apk..")

    //extracting the dex files from the apk
    val extractor:Plugin = new Extractor
    extractor.apply(context, Array(Constant.REGEX_PATTERN_DEX,"intermediate-assembly.dex"))
    context = extractor.result

    println("extracted dex: " + context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX))

    println("trying to disassemble dex files..")

    val smali:Plugin = new SmaliDisassembler
    smali.apply(context,Array())
    context = smali.result

    println("disassembled smali: " + context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_SMALI))

    val grep:Plugin = new GrepAnalyser

    //TODO: the regex is still too broad
    grep.apply(context,Array(Constant.REGEX_PATTERN_EMAIL,"intermediate-assembly.smali","result-log.grep-login"))
    context = grep.result

  }
}
