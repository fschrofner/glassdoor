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
    val file = new File(Constant.Config.Path.CONFIG_FILE)
    mConfig = Some(ConfigFactory.parseFile(file));
  }

  def loadConfigIntoContext(context:Context): Context ={
    var map = context.getKeymapMatchingString(Constant.Context.Keymap.CONFIG)

    val conf = getConfigObject(Constant.Config.ConfigKey.DEFAULT_KEY)
    val configSet = conf.get.entrySet().asScala

    for(entry:Entry[String,ConfigValue] <- configSet){
      map += ((entry.getKey, String.valueOf(entry.getValue.unwrapped())))
    }

    context.setKeymapMatchingString(Constant.Context.Keymap.CONFIG, map)
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
