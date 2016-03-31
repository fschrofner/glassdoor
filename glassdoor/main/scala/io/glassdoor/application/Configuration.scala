package io.glassdoor.application

import java.io.File

import com.typesafe.config.{ConfigException, Config, ConfigFactory}

/**
  * Created by Florian Schrofner on 3/31/16.
  */
object Configuration {
  var mConfig:Option[Config] = None

  def loadConfig():Unit = {
    val file = new File(Constant.CONFIG_FILE_PATH)
    mConfig = Some(ConfigFactory.parseFile(file));
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
