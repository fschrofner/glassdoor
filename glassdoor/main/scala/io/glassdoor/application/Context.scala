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
    }
  }

  //TODO: abstract keymaps behind more, provide context interface
  //TODO: provide method to load a flat file list of a certain keydescriptor
}
