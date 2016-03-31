package io.glassdoor.application

/**
  * Created by Florian Schrofner on 3/17/16.
  */
object Constant {
  //these are string representations of the possible keymaps
  val DESCRIPTOR_SPLIT = """\."""
  val ORIGINAL_BINARY = "original-binary"
  val INTERMEDIATE_ASSEMBLY = "intermediate-assembly"
  val INTERMEDIATE_SOURCE = "intermediate-source"
  val INTERMEDIATE_RESOURCES = "intermediate-resource"
  val RESULT_LOG = "result-log"

  //these are the context keys for the included plugins
  val ORIGINAL_BINARY_APK = "apk"
  val INTERMEDIATE_ASSEMBLY_DEX = "dex"
  val INTERMEDIATE_ASSEMBLY_SMALI = "smali"
  val RESULT_LOG_GREP_LOGIN = "grep-login"

  //predefined regexes
  val REGEX_PATTERN_DEX = """^.*\.[Dd][Ee][Xx]$"""
  val REGEX_PATTERN_EMAIL = """.+@.+\..{2,}"""

  //TODO: these values should be retrieved by the config somehow
  val ROOT_WORKING_DIRECTORY = "/home/flosch/glassdoor-testset/"
  val ROOT_DICTIONARY_DIRECTORY = "/home/flosch/glassdoor/dictionaries"

  val CONFIG_FILE_PATH = "/home/flosch/Projects/glassdoor/glassdoor.conf"

  //val URL_DICTIONARY_REPO = ""
  //val URL_PLUGIN_REPO = ""
}
