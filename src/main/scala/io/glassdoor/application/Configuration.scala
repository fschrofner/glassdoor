package io.glassdoor.application

import java.io.{File, PrintWriter}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.Files.copy
import java.nio.file.Paths.get
import java.util
import java.util.Map.Entry

import scala.collection.JavaConverters._
import com.typesafe.config._

/**
  * This object handles everything that has to do with the configuration.
  * Created by Florian Schrofner on 3/31/16.
  */
object Configuration {
  var mConfig:Option[Config] = None

  /**
    * Loads the config from the path specified in xdg_config_home.
    * If not specified, the default path $HOME/.config/glassdoor is used
    */
  def loadConfig():Unit = {
    val configDirEnv = sys.env.get("XDG_CONFIG_HOME")
    var configDir:String = null
    var configFile:File = null

    if(configDirEnv.isDefined){
      configDir = configDirEnv.get + ConfigConstant.Path.GlassdoorSubDir
    } else {
      configDir = ConfigConstant.Path.DefaultConfigDir + ConfigConstant.Path.GlassdoorSubDir
    }

    configFile = new File(configDir + ConfigConstant.Path.ConfigFileName)

    if(!configFile.exists()){
      setupDefaultConfig(configFile)
    }
    val config = ConfigFactory.parseFile(configFile)
    mConfig = Some(config.resolve())
  }

  /**
    * This method should be called, if there is no config file yet.
    * It will create a new configuration file at the specified location.
    * Additionally it will copy the default plugins and alias configuration to the same directory
    * and adapt the config file accordingly.
    * @param configFile
    */
  def setupDefaultConfig(configFile:File): Unit ={
    configFile.getParentFile.mkdirs()
    val configDir = configFile.getParentFile.getAbsolutePath

    Log.debug("resource config file stored in: " + getClass.getResource(ConfigConstant.Path.ConfigFileName).getPath)

    val configResource = getClass.getResourceAsStream(ConfigConstant.Path.ConfigFileName)
    val aliasResource = getClass.getResourceAsStream(ConfigConstant.Path.AliasFileName)
    val pluginsResource = getClass.getResourceAsStream(ConfigConstant.Path.PluginsFileName)


    val configPath = configDir + ConfigConstant.Path.ConfigFileName
    val aliasPath = configDir + ConfigConstant.Path.AliasFileName
    val pluginsPath = configDir + ConfigConstant.Path.PluginsFileName

    copy(configResource, get(configPath), REPLACE_EXISTING)
    copy(aliasResource, get(aliasPath), REPLACE_EXISTING)
    copy(pluginsResource, get(pluginsPath), REPLACE_EXISTING)

    Log.debug("copied config file to: " + configDir + File.separator + ConfigConstant.Path.ConfigFileName)

    //set paths in config file correctly
    val config = ConfigFactory.parseFile(configFile)

    val adaptedConfig = config.withValue(ConfigConstant.ConfigKey.FullKey.AliasConfigPath,ConfigValueFactory.fromAnyRef(aliasPath)).
      withValue(ConfigConstant.ConfigKey.FullKey.PluginConfigPath,ConfigValueFactory.fromAnyRef(pluginsPath)).
      withValue(ConfigConstant.ConfigKey.FullKey.RootDirectory, ConfigValueFactory.fromAnyRef(sys.env("HOME") + ConfigConstant.Path.GlassdoorSubDir))

    new PrintWriter(configFile) {
      val renderOptions = ConfigRenderOptions.defaults().setComments(false).setOriginComments(false)
      write(adaptedConfig.root().render(renderOptions))
      close()
    }

    Log.debug("adapted paths of alias.conf and plugins.conf inside glassdoor.conf")
  }

  def loadConfigIntoContext(context:io.glassdoor.application.Context): Option[Context] ={
    var result:Option[Context] = None
    val mapOpt = context.getKeymapMatchingString(ContextConstant.Keymap.Config)

    if(mapOpt.isDefined){
      var map = mapOpt.get
      val conf = getConfigObject(ConfigConstant.ConfigKey.DefaultKey)
      val configSet = conf.get.entrySet().asScala

      for(entry:Entry[String,ConfigValue] <- configSet){
        if(entry.getValue.valueType() == ConfigValueType.STRING){
          map += ((entry.getKey, String.valueOf(entry.getValue.unwrapped())))
        } else if(entry.getValue.valueType() == ConfigValueType.LIST){
          Log.debug("config value: " + String.valueOf(entry.getValue.unwrapped()))
          Log.debug("type: " + String.valueOf(entry.getValue.valueType()))
          val list = entry.getValue.unwrapped().asInstanceOf[util.ArrayList[String]].asScala
          val resolvedValue = context.arrayToString(list.toArray)
          Log.debug("resolved value: " + resolvedValue)
          map += ((entry.getKey, resolvedValue))
        }
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
    val GlassdoorSubDir = File.separator + "glassdoor"
    val DefaultConfigDir = sys.env("HOME") + File.separator + ".config"
    val AliasFileName = File.separator + "alias.conf"
    val PluginsFileName = File.separator + "plugins.conf"
    val ConfigFileName = File.separator + "glassdoor.conf"
  }

  object ConfigKey {
    val DefaultKey = "glassdoor"

    object Key {
      val DefaultPlugins = "defaultPlugins"
      val Aliases = "aliases"
      val WorkingDirectory = "workingDirectory"
      val ResourceDirectory = "resourceDirectory"
      val RootDirectory = "rootDirectory"
      val PluginDirectory = "pluginDirectory"
      val PluginConfigPath = "pluginConfigPath"
      val AliasConfigPath ="aliasConfigPath"
      val PluginRepository = "pluginRepository"
      val ResourceRepository = "resourceRepository"
      val EmulatorRepositoryPath = "emulatorRepositoryPath"
      val AndroidSdkPath = "androidSdkPath"
    }

    object FullKey {
      val DefaultPlugins = DefaultKey + DescriptorSplit + Key.DefaultPlugins
      val Aliases = DefaultKey + DescriptorSplit + Key.Aliases
      val WorkingDirectory = DefaultKey + DescriptorSplit + Key.WorkingDirectory
      val ResourceDirectory = DefaultKey + DescriptorSplit + Key.ResourceDirectory
      val RootDirectory = DefaultKey + DescriptorSplit + Key.RootDirectory
      val PluginDirectory = DefaultKey + DescriptorSplit + Key.PluginDirectory
      val PluginConfigPath = DefaultKey + DescriptorSplit + Key.PluginConfigPath
      val AliasConfigPath = DefaultKey + DescriptorSplit + Key.AliasConfigPath
      val PluginRepository = DefaultKey + DescriptorSplit + Key.PluginRepository
      val ResourceRepository = DefaultKey + DescriptorSplit + Key.ResourceRepository
      val EmulatorRepositoryPath = DefaultKey + DescriptorSplit + Key.EmulatorRepositoryPath
      val AndroidSdkPath = DefaultKey + DescriptorSplit + Key.AndroidSdkPath
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
    val Interpreter = "interpreter"
    val Help = "help"
  }

  object AliasKey  {
    val Shorthand = "shorthand"
    val Commands = "commands"
  }
}
