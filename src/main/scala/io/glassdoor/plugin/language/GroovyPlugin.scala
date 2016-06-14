package io.glassdoor.plugin.language

import java.io.File

import akka.actor.Actor
import akka.actor.Actor.Receive
import groovy.lang.{GroovyClassLoader, GroovyObject}
import io.glassdoor.application.{Log, Context}
import io.glassdoor.bus.Message
import io.glassdoor.plugin.Plugin

/**
  * Wrapper to launch Groovy plugins. Handles all communication with the PluginManager and the actor system.
  * Created by Florian Schrofner on 4/19/16.
  */
class GroovyPlugin extends Plugin {
  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    val plugin = instantiateGroovyPlugin("/home/flosch/Projects/groovy-test/main.groovy")
    if(plugin.isDefined){
      val output = plugin.get.invokeMethod("saySomething",null)
      //Log.debug(output)
    }
  }

  override def result: Option[Map[String,String]] = ???

  override def help(parameters: Array[String]): Unit = ???

  def instantiateGroovyPlugin(fileName:String):Option[GroovyObject] = {
    val file = new File(fileName)

    if(file.exists()){
      //TODO: probably persist class loader
      //TODO: also handle instantiation errors here
      val classLoader = new GroovyClassLoader()
      val groovyClass = classLoader.parseClass(file)
      val groovyObject = groovyClass.newInstance().asInstanceOf[GroovyObject]
      Some(groovyObject)
    } else {
      None
    }
  }
}
