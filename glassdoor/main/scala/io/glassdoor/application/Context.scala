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
      case Constant.ORIGINAL_BINARY =>
        return originalBinary
      case Constant.INTERMEDIATE_ASSEMBLY =>
        return intermediateAssembly
      case Constant.INTERMEDIATE_SOURCE =>
        return intermediateSource
      case Constant.INTERMEDIATE_RESOURCES =>
        return intermediateResource
      case Constant.RESULT_LOG =>
        return resultLog
      case Constant.CONTEXT_CONFIG =>
        return configuration
    }
  }

  def setKeymapMatchingString(keymapDescription:String, keymap:Map[String,String]): Unit = {
    keymapDescription match {
      case Constant.ORIGINAL_BINARY =>
        originalBinary = keymap
      case Constant.INTERMEDIATE_ASSEMBLY =>
        intermediateAssembly = keymap
      case Constant.INTERMEDIATE_SOURCE =>
        intermediateSource = keymap
      case Constant.INTERMEDIATE_RESOURCES =>
        intermediateResource = keymap
      case Constant.RESULT_LOG =>
        resultLog = keymap
      case Constant.CONTEXT_CONFIG =>
        configuration = keymap
    }
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.DESCRIPTOR_SPLIT)
  }

  def getResolvedValue(descriptor:String): Option[String] ={
    val descriptorSplitString = splitDescriptor(descriptor)

    val keymapDescriptor = descriptorSplitString(0)
    val keyDescriptor = descriptorSplitString(1)

    val keymap = getKeymapMatchingString(keymapDescriptor)
    val result = keymap.get(keyDescriptor)
    result
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
