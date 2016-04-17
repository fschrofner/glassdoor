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
  private var resultLog:Map[String, String] = new HashMap[String, String]
  private var resource:Map[String,String] = new HashMap[String,String]
  private var configuration:Map[String,String] = new HashMap[String,String]

  def getKeymapMatchingString(keymapDescription:String):Map[String,String] = {
    keymapDescription match {
      case ContextConstant.Keymap.ORIGINAL_BINARY =>
        return originalBinary
      case ContextConstant.Keymap.INTERMEDIATE_ASSEMBLY =>
        return intermediateAssembly
      case ContextConstant.Keymap.INTERMEDIATE_SOURCE =>
        return intermediateSource
      case ContextConstant.Keymap.INTERMEDIATE_RESOURCES =>
        return intermediateResource
      case ContextConstant.Keymap.RESULT_LOG =>
        return resultLog
      case ContextConstant.Keymap.RESOURCE =>
        return resource
      case ContextConstant.Keymap.CONFIG =>
        return configuration
    }
  }

  def setKeymapMatchingString(keymapDescription:String, keymap:Map[String,String]): Unit = {
    keymapDescription match {
      case ContextConstant.Keymap.ORIGINAL_BINARY =>
        originalBinary = keymap
      case ContextConstant.Keymap.INTERMEDIATE_ASSEMBLY =>
        intermediateAssembly = keymap
      case ContextConstant.Keymap.INTERMEDIATE_SOURCE =>
        intermediateSource = keymap
      case ContextConstant.Keymap.INTERMEDIATE_RESOURCES =>
        intermediateResource = keymap
      case ContextConstant.Keymap.RESULT_LOG =>
        resultLog = keymap
      case ContextConstant.Keymap.RESOURCE =>
        resource = keymap
      case ContextConstant.Keymap.CONFIG =>
        configuration = keymap
    }
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DESCRIPTOR_SPLIT_REGEX)
  }

  def getResolvedValue(descriptor:String): Option[String] ={
    val descriptorSplitString = splitDescriptor(descriptor)

    try {
      val keymapDescriptor = descriptorSplitString(0)
      val keyDescriptor = descriptorSplitString(1)

      val keymap = getKeymapMatchingString(keymapDescriptor)
      val result = keymap.get(keyDescriptor)
      result
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        None
    }

  }

  def setResolvedValue(descriptor:String, value:String):Context = {
    val descriptorSplitString = splitDescriptor(descriptor)
    val keymapDescriptor = descriptorSplitString(0)
    val keyDescriptor = descriptorSplitString(1)

    var keymap:Map[String,String] = getKeymapMatchingString(keymapDescriptor)
    keymap = keymap + ((keyDescriptor,value))

    setKeymapMatchingString(keymapDescriptor,keymap)
    this
  }

  //TODO: abstract keymaps behind more, provide context interface
}

object ContextConstant {
  val DESCRIPTOR_SPLIT = "."

  //keymap names
  object Keymap {
    val ORIGINAL_BINARY = "original-binary"
    val INTERMEDIATE_ASSEMBLY = "intermediate-assembly"
    val INTERMEDIATE_SOURCE = "intermediate-source"
    val INTERMEDIATE_RESOURCES = "intermediate-resource"
    val RESULT_LOG = "result-log"
    val CONFIG = "config"
    val RESOURCE = "resource"
  }

  //these are the key values used inside the keymaps
  object Key {
    val APK = "apk"
    val DEX = "dex"
    val SMALI = "smali"
    val GREP = "grep"
    val GREP_LOGIN = "grep-login"
  }

  //keys defining the keymap and the keys in one string
  object FullKey {
    val ORIGINAL_BINARY_APK = Keymap.ORIGINAL_BINARY + DESCRIPTOR_SPLIT + Key.APK
    val INTERMEDIATE_ASSEMBLY_DEX = Keymap.INTERMEDIATE_ASSEMBLY + DESCRIPTOR_SPLIT + Key.DEX
    val INTERMEDIATE_ASSEMBLY_SMALI = Keymap.INTERMEDIATE_ASSEMBLY + DESCRIPTOR_SPLIT + Key.SMALI
    val RESULT_LOG_GREP_LOGIN = Keymap.RESULT_LOG + DESCRIPTOR_SPLIT + Key.GREP_LOGIN
    val CONFIG_WORKING_DIRECTORY = Keymap.CONFIG + DESCRIPTOR_SPLIT + ConfigConstant.ConfigKey.Key.WORKING_DIRECTORY
    val CONFIG_PLUGIN_CONFIG_PATH = Keymap.CONFIG + DESCRIPTOR_SPLIT + ConfigConstant.ConfigKey.Key.PLUGIN_CONFIG_PATH
  }
}