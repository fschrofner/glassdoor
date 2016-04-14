package io.glassdoor.application

/**
  * Created by Florian Schrofner on 3/17/16.
  */
object Constant {
  val DESCRIPTOR_SPLIT = "."

  //TODO: these values should be retrieved by the config somehow
  val ROOT_WORKING_DIRECTORY = "/home/flosch/glassdoor-testset/"
  val ROOT_DICTIONARY_DIRECTORY = "/home/flosch/glassdoor/dictionaries"

  //these are string representations of the possible keymaps
  object Context {
    //keymap names
    object Keymap {
      val ORIGINAL_BINARY = "original-binary"
      val INTERMEDIATE_ASSEMBLY = "intermediate-assembly"
      val INTERMEDIATE_SOURCE = "intermediate-source"
      val INTERMEDIATE_RESOURCES = "intermediate-resource"
      val RESULT_LOG = "result-log"
      val CONFIG = "config"
    }

    //these are the key values used inside the keymaps
    object Key {
      val APK = "apk"
      val DEX = "dex"
      val SMALI = "smali"
      val GREP_LOGIN = "grep-login"
    }

    //keys defining the keymap and the keys in one string
    object FullKey {
      val ORIGINAL_BINARY_APK = Keymap.ORIGINAL_BINARY + DESCRIPTOR_SPLIT + Key.APK
      val INTERMEDIATE_ASSEMBLY_DEX = Keymap.INTERMEDIATE_ASSEMBLY + DESCRIPTOR_SPLIT + Key.DEX
      val INTERMEDIATE_ASSEMBLY_SMALI = Keymap.INTERMEDIATE_ASSEMBLY + DESCRIPTOR_SPLIT + Key.SMALI
      val RESULT_LOG_GREP_LOGIN = Keymap.RESULT_LOG + DESCRIPTOR_SPLIT + Key.GREP_LOGIN
      val CONFIG_WORKING_DIRECTORY = Keymap.CONFIG + DESCRIPTOR_SPLIT + Config.ConfigKey.Key.WORKING_DIRECTORY
      val CONFIG_PLUGIN_CONFIG_PATH = Keymap.CONFIG + DESCRIPTOR_SPLIT + Config.ConfigKey.Key.PLUGIN_CONFIG_PATH
    }
  }

  //predefined regexes
  object Regex {
    val DESCRIPTOR_SPLIT_REGEX = """\."""
    val REGEX_PATTERN_DEX = """^.*\.[Dd][Ee][Xx]$"""
    val REGEX_PATTERN_EMAIL = """.+@.+\..{2,}"""
  }

  //config keys
  object Config {

    object Path {
      //TODO: this needs to be adapted dynamically
      val CONFIG_FILE = "/home/flosch/Projects/glassdoor/conf/glassdoor.conf"
    }

    object ConfigKey {
      val DEFAULT_KEY = "glassdoor"

      object Key {
        val DEFAULT_PLUGINS = "defaultPlugins"
        val WORKING_DIRECTORY = "workingDirectory"
        val PLUGIN_CONFIG_PATH = "pluginConfigPath"
      }

      object FullKey {
        val DEFAULT_PLUGINS = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.DEFAULT_PLUGINS
        val WORKING_DIRECTORY = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.WORKING_DIRECTORY
        val PLUGIN_CONFIG_PATH = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.PLUGIN_CONFIG_PATH
      }

    }

    //values describing a certain plugin
    object PluginKey {
      val NAME = "name"
      val TYPE = "type"
      val DEPENDENCIES = "dependencies"
      val COMMANDS = "commands"
      val CLASSFILE = "classFile"
    }
  }

  //these are the context keys for accessing config values
  //val CONTEXT_WORKING_DIRECTORY = CONTEXT_CONFIG + "." + CONFIG_WORKING_DIRECTORY

  //val URL_DICTIONARY_REPO = ""
  //val URL_PLUGIN_REPO = ""
}
