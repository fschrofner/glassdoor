package io.glassdoor.application

/**
  * Created by Florian Schrofner on 3/17/16.
  */
object Constant {
  //these are string representations of the possible keymaps
  val ORIGINAL_BINARY = "original-binary"
  val INTERMEDIATE_ASSEMBLY = "intermediate-assembly"
  val INTERMEDIATE_SOURCE = "intermediate-source"
  val INTERMEDIATE_RESOURCES = "intermediate-resource"
  val RESULT_LOG = "result-log"

  //these are the context keys for the included plugins
  val ORIGINAL_BINARY_APK = "apk"
  val INTERMEDIATE_ASSEMBLY_DEX = "dex"
  val INTERMEDIATE_ASSEMBLY_SMALI = "smali"

  //predefined regexes for the extractor
  val REGEX_PATTERN_DEX = """^.*\.[Dd][Ee][Xx]$"""

  //TODO: these values should be retrieved by the config somehow
  val ROOT_DIRECTORY = "/home/flosch/glassdoor-testset/"
}
