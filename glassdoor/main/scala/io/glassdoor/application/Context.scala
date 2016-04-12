package io.glassdoor.application

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/16/16.
  */
class Context {
  var originalBinary:Map[String, String] = new HashMap[String, String]
  var intermediateSource:Map[String, String] = new HashMap[String, String]
  var intermediateAssembly:Map[String, String] = new HashMap[String, String]
  var intermediateResource:Map[String, String] = new HashMap[String, String]
  var resultLog:Map[String, String] = new HashMap[String, String]
  var configuration:Map[String,String] = new HashMap[String,String]

  def getKeymapMatchingString(keymapDescription:String):Map[String,String] = {
    keymapDescription match {
      case Constant.Context.Keymap.ORIGINAL_BINARY =>
        return originalBinary
      case Constant.Context.Keymap.INTERMEDIATE_ASSEMBLY =>
        return intermediateAssembly
      case Constant.Context.Keymap.INTERMEDIATE_SOURCE =>
        return intermediateSource
      case Constant.Context.Keymap.INTERMEDIATE_RESOURCES =>
        return intermediateResource
      case Constant.Context.Keymap.RESULT_LOG =>
        return resultLog
      case Constant.Context.Keymap.CONFIG =>
        return configuration
    }
  }

  def setKeymapMatchingString(keymapDescription:String, keymap:Map[String,String]): Unit = {
    keymapDescription match {
      case Constant.Context.Keymap.ORIGINAL_BINARY =>
        originalBinary = keymap
      case Constant.Context.Keymap.INTERMEDIATE_ASSEMBLY =>
        intermediateAssembly = keymap
      case Constant.Context.Keymap.INTERMEDIATE_SOURCE =>
        intermediateSource = keymap
      case Constant.Context.Keymap.INTERMEDIATE_RESOURCES =>
        intermediateResource = keymap
      case Constant.Context.Keymap.RESULT_LOG =>
        resultLog = keymap
      case Constant.Context.Keymap.CONFIG =>
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
