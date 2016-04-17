package io.glassdoor.application

import java.io.File
import java.util.Map.Entry
import scala.collection.JavaConverters._

import com.typesafe.config.{ConfigValue, ConfigException, Config, ConfigFactory}

/**
  * Created by Florian Schrofner on 3/31/16.
  */
object Configuration {
  var mConfig:Option[Config] = None

  def loadConfig():Unit = {
    val file = new File(ConfigConstant.Path.CONFIG_FILE)
    mConfig = Some(ConfigFactory.parseFile(file));
  }

  def loadConfigIntoContext(context:io.glassdoor.application.Context): Context ={
    var map = context.getKeymapMatchingString(ContextConstant.Keymap.CONFIG)

    val conf = getConfigObject(ConfigConstant.ConfigKey.DEFAULT_KEY)
    val configSet = conf.get.entrySet().asScala

    for(entry:Entry[String,ConfigValue] <- configSet){
      map += ((entry.getKey, String.valueOf(entry.getValue.unwrapped())))
    }

    context.setKeymapMatchingString(ContextConstant.Keymap.CONFIG, map)
    context
  }

  def getConfigObject(key:String):Option[Config] = {
    if(mConfig.isDefined){
      try {
        val result = mConfig.get.getConfig(key)
        return Some(result)
      } catch {
        case e:ConfigException =>
          return None
      }
    } else {
      return None
    }
  }
  def getString(key:String):Option[String] = {
    if(mConfig.isDefined){
      try {
        val result = mConfig.get.getString(key)
        if(result != null){
          return Some(result)
        } else {
          return None
        }
      } catch {
        case e:ConfigException =>
          return None
      }

    } else {
      return None
    }
  }
}

object ConfigConstant {
  val DESCRIPTOR_SPLIT = "."

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
      val PLUGIN_REPOSITORY = "pluginRepository"
      val RESOURCE_REPOSITORY = "resourceRepository"
    }

    object FullKey {
      val DEFAULT_PLUGINS = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.DEFAULT_PLUGINS
      val WORKING_DIRECTORY = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.WORKING_DIRECTORY
      val PLUGIN_CONFIG_PATH = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.PLUGIN_CONFIG_PATH
      val PLUGIN_REPOSITORY = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.PLUGIN_REPOSITORY
      val RESOURCE_REPOSITORY = DEFAULT_KEY + DESCRIPTOR_SPLIT + Key.RESOURCE_REPOSITORY
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