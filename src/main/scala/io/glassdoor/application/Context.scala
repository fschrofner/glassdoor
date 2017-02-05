package io.glassdoor.application

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/16/16.
  */
class Context {
  private var originalBinary:Map[String, String] = new HashMap[String, String]
  private var intermediateSource:Map[String, String] = new HashMap[String, String]
  private var intermediateAssembly:Map[String, String] = new HashMap[String, String]
  private var intermediateResource:Map[String, String] = new HashMap[String, String]
  private var dynamicAnalysis:Map[String, String] = new HashMap[String, String]
  private var resultLog:Map[String, String] = new HashMap[String, String]
  private var resource:Map[String,String] = new HashMap[String,String]
  private var configuration:Map[String,String] = new HashMap[String,String]

  def getKeymapMatchingString(keymapDescription:String):Option[Map[String,String]] = {
    var result:Option[Map[String,String]] = None

    keymapDescription match {
      case ContextConstant.Keymap.OriginalBinary =>
        result = Some(originalBinary)
      case ContextConstant.Keymap.IntermediateAssembly =>
        result = Some(intermediateAssembly)
      case ContextConstant.Keymap.IntermediateSource =>
        result = Some(intermediateSource)
      case ContextConstant.Keymap.IntermediateResource =>
        result = Some(intermediateResource)
      case ContextConstant.Keymap.DynamicAnalysis =>
        result = Some(dynamicAnalysis)
      case ContextConstant.Keymap.ResultLog =>
        result = Some(resultLog)
      case ContextConstant.Keymap.Resource =>
        result = Some(resource)
      case ContextConstant.Keymap.Config =>
        result = Some(configuration)
      case _ =>
        result = None
    }
    result
  }

  def setKeymapMatchingString(keymapDescription:String, keymap:Map[String,String]): Unit = {
    keymapDescription match {
      case ContextConstant.Keymap.OriginalBinary =>
        originalBinary = keymap
      case ContextConstant.Keymap.IntermediateAssembly =>
        intermediateAssembly = keymap
      case ContextConstant.Keymap.IntermediateSource =>
        intermediateSource = keymap
      case ContextConstant.Keymap.IntermediateResource =>
        intermediateResource = keymap
      case ContextConstant.Keymap.DynamicAnalysis =>
        dynamicAnalysis = keymap
      case ContextConstant.Keymap.ResultLog =>
        resultLog = keymap
      case ContextConstant.Keymap.Resource =>
        resource = keymap
      case ContextConstant.Keymap.Config =>
        configuration = keymap
      case _ =>
        //TODO: error!
    }
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    Log.debug("splitting: " + descriptor)
    if(descriptor.contains(ContextConstant.DescriptorSplit)){
      val prefix = descriptor.substring(0, descriptor.indexOf(ContextConstant.DescriptorSplit))
      val name = descriptor.substring(descriptor.indexOf(ContextConstant.DescriptorSplit) + 1, descriptor.length)
      //descriptor.split(Constant.Regex.DescriptorSplitRegex)
      Array(prefix, name)
    } else {
      Array(descriptor)
    }

  }

  def getResolvedValue(descriptor:String): Option[String] ={
    val descriptorSplitString = splitDescriptor(descriptor)

    try {
      val keymapDescriptor = descriptorSplitString(0)
      val keyDescriptor = descriptorSplitString(1)

      val keymapOpt = getKeymapMatchingString(keymapDescriptor)

      if(keymapOpt.isDefined){
        val result = keymapOpt.get.get(keyDescriptor)
        result
      } else {
        None
      }
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        None
    }
  }

  def getResolvedArray(descriptor:String):Option[Array[String]] ={
    val string = getResolvedValue(descriptor)
    if(string.isDefined){
      Some(stringToArray(string.get))
    } else {
      None
    }
  }

  def arrayToString(array:Array[String]):String={
    array.mkString(ContextConstant.StringArraySplit)
  }

  def stringToArray(string:String):Array[String]={
    string.split(ContextConstant.StringArraySplit)
  }


  def setResolvedValue(descriptor:String, value:String):Option[Context] = {
    val descriptorSplitString = splitDescriptor(descriptor)
    val keymapDescriptor = descriptorSplitString(0)
    val keyDescriptor = descriptorSplitString(1)

    val keymapOpt = getKeymapMatchingString(keymapDescriptor)

    if(keymapOpt.isDefined){
      var keymap = keymapOpt.get
      keymap = keymap + ((keyDescriptor,value))
      setKeymapMatchingString(keymapDescriptor,keymap)
      Some(this)
    } else {
      None
    }
  }

  def removeResolvedValue(descriptor:String):Option[Context] = {
    val descriptorSplitString = splitDescriptor(descriptor)
    val keymapDescriptor = descriptorSplitString(0)
    val keyDescriptor = descriptorSplitString(1)

    val keymapOpt = getKeymapMatchingString(keymapDescriptor)

    if(keymapOpt.isDefined){
      var keymap = keymapOpt.get
      keymap = keymap - keyDescriptor
      setKeymapMatchingString(keymapDescriptor,keymap)
      Some(this)
    } else {
      None
    }
  }

  //TODO: abstract keymaps behind more, provide context interface
}

object ContextConstant {
  val DescriptorSplit = "."
  val StringArraySplit = ","

  //keymap names
  object Keymap {
    val OriginalBinary = "original-binary"
    val IntermediateAssembly = "intermediate-assembly"
    val IntermediateSource = "intermediate-source"
    val IntermediateResource = "intermediate-resource"
    val DynamicAnalysis = "dynamic-analysis"
    val ResultLog = "result-log"
    val Config = "config"
    val Resource = "resource"
  }

  //these are the key values used inside the keymaps
  object Key {
    val Aapt = "aapt"
    val Apk = "apk"
    val Dex = "dex"
    val Smali = "smali"
    val Java = "java"
    val Regex = "regex"
    val HashCrack = "hashcrack"
    val RegexLogin = "regex-login"
    val Dictionary = "dictionary"
    val ExtractedDatabase = "extracted-database"
    val Emulator = "emulator"
    val Mitm = "mitm"
    val Tracer = "tracer"
  }

  //keys defining the keymap and the keys in one string
  object FullKey {
    val OriginalBinaryApk = Keymap.OriginalBinary + DescriptorSplit + Key.Apk
    val IntermediateAssemblyDex = Keymap.IntermediateAssembly + DescriptorSplit + Key.Dex
    val IntermediateAssemblySmali = Keymap.IntermediateAssembly + DescriptorSplit + Key.Smali
    val IntermediateSourceJava = Keymap.IntermediateSource + DescriptorSplit + Key.Java
    val ResultLogRegexLogin = Keymap.ResultLog + DescriptorSplit + Key.RegexLogin
    val ResultLogHashCrack = Keymap.ResultLog + DescriptorSplit + Key.HashCrack
    val ResultLogMitm = Keymap.ResultLog + DescriptorSplit + Key.Mitm
    val ResultLogTracer = Keymap.ResultLog + DescriptorSplit + Key.Tracer
    val ConfigWorkingDirectory = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.WorkingDirectory
    val ConfigPluginConfigPath = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.PluginConfigPath
    val ConfigPluginDirectory = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.PluginDirectory
    val ConfigAliasConfigPath = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.AliasConfigPath
    val ConfigResourceDirectory = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.ResourceDirectory
    val ConfigResourceRepository = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.ResourceRepository
    val ConfigEmulatorRepositoryPath = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.EmulatorRepositoryPath
    val ConfigAndroidSdkPath = Keymap.Config + DescriptorSplit + ConfigConstant.ConfigKey.Key.AndroidSdkPath
    val ResourceDictionary = Keymap.Resource + DescriptorSplit + Key.Dictionary
    val DynamicAnalysisEmulator = Keymap.DynamicAnalysis + DescriptorSplit + Key.Emulator
    val DynamicAnalysisMitm = Keymap.DynamicAnalysis + DescriptorSplit + Key.Mitm
    val DynamicAnalysisTracer = Keymap.DynamicAnalysis + DescriptorSplit + Key.Tracer
  }
}
