package io.glassdoor.plugin.plugins.analyser.hash

import io.glassdoor.plugin.plugins.analyser.hash.HashAlgorithm.HashAlgorithm
import io.glassdoor.plugin.plugins.analyser.hash.HashCrackerBackend.HashCrackerBackend

/**
  * Created by flosch on 7/5/16.
  */
class HashCrackerOptions {
  var hashCrackerBackend:HashCrackerBackend = HashCrackerBackend.John
  var singleHash:Boolean = false
  var hash:Option[String] = None
  var dictionary:Option[String] = None
  var dictionarySubFile:Option[String] = None
  var hashAlgorithm:Option[HashAlgorithm] = None
}

object HashCrackerBackend extends Enumeration {
  type HashCrackerBackend = Value
  val John,HashCat = Value
}

object HashAlgorithm extends Enumeration {
  type HashAlgorithm = Value
  val Md5,Sha256 = Value
}