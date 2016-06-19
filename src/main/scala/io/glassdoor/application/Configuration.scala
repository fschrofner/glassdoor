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
    val file = new File(ConfigConstant.Path.ConfigFile)
    mConfig = Some(ConfigFactory.parseFile(file))
  }

  def loadConfigIntoContext(context:io.glassdoor.application.Context): Option[Context] ={
    var result:Option[Context] = None
    val mapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.Config)

    if(mapOpt.isDefined){
      var map = mapOpt.get
      val conf = getConfigObject(ConfigConstant.ConfigKey.DefaultKey)
      val configSet = conf.get.entrySet().asScala

      for(entry:Entry[String,ConfigValue] <- configSet){
        map += ((entry.getKey, String.valueOf(entry.getValue.unwrapped())))
      }

      context.setKeymapMatchingString(ContextConstant.Keymap.Config, map)
      result = Some(context)
    }

    result
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
  val DescriptorSplit = "."

  object Path {
    //TODO: this needs to be adapted dynamically
    val ConfigFile = "/home/flosch/Projects/glassdoor/conf/glassdoor.conf"
  }

  object ConfigKey {
    val DefaultKey = "glassdoor"

    object Key {
      val DefaultPlugins = "defaultPlugins"
      val Aliases = "aliases"
      val WorkingDirectory = "workingDirectory"
      val ResourceDirectory = "resourceDirectory"
      val PluginConfigPath = "pluginConfigPath"
      val AliasConfigPath ="aliasConfigPath"
      val PluginRepository = "pluginRepository"
      val ResourceRepository = "resourceRepository"
    }

    object FullKey {
      val DefaultPlugins = DefaultKey + DescriptorSplit + Key.DefaultPlugins
      val Aliases = DefaultKey + DescriptorSplit + Key.Aliases
      val WorkingDirectory = DefaultKey + DescriptorSplit + Key.WorkingDirectory
      val ResourceDirectory = DefaultKey + DescriptorSplit + Key.ResourceDirectory
      val PluginConfigPath = DefaultKey + DescriptorSplit + Key.PluginConfigPath
      val AliasConfigPath = DefaultKey + DescriptorSplit + Key.AliasConfigPath
      val PluginRepository = DefaultKey + DescriptorSplit + Key.PluginRepository
      val ResourceRepository = DefaultKey + DescriptorSplit + Key.ResourceRepository
    }

  }

  //values describing a certain plugin
  object PluginKey {
    val Name = "name"
    val Type = "type"
    val Dependencies = "dependencies"
    val Changes = "changes"
    val Commands = "commands"
    val ClassFile = "classFile"
  }

  object AliasKey  {
    val Shorthand = "shorthand"
    val Commands = "commands"
  }
}
