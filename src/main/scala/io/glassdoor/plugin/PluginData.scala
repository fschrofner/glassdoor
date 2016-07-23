package io.glassdoor.plugin

/**
  * Class containing all the information required to instantiate a plugin.
  */
case class PluginData(name:String, kind:String, dependencies:Array[String], changes:Array[String], commands:Array[String], pluginClass:String, pluginEnvironment:Option[Map[String,String]], help:String)

object PluginEnvironmentConstant {
  object Key {
    val MainClass = "mainClass"
  }
  object Interpreter {
    val Groovy = "groovy"
  }
}