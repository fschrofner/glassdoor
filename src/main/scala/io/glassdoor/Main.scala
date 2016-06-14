package io.glassdoor

import akka.actor.{Props, ActorSystem}
import io.glassdoor.application._
import io.glassdoor.bus.{MessageEvent, Message, EventBus}
import io.glassdoor.controller.{DefaultController, ControllerConstant, Controller}
import io.glassdoor.interface.{UserInterfaceConstant, UserInterface, CommandLineInterface}
import io.glassdoor.plugin.manager.{PluginManagerConstant, DefaultPluginManager}
import io.glassdoor.plugin.resource.{DefaultResourceManager, ResourceManagerConstant}
import io.glassdoor.plugin.Plugin
import io.glassdoor.plugin.plugins.analyser.grep.GrepAnalyser
import io.glassdoor.plugin.plugins.loader.apk.ApkLoader
import io.glassdoor.plugin.plugins.preprocessor.extractor.Extractor
import io.glassdoor.plugin.plugins.preprocessor.smali.SmaliDisassembler

object Main {

  def main(args:Array[String]):Unit={
    val system = ActorSystem()

    Log.debug("the first line of glassdoor!")

    //create components
    val controller = system.actorOf(Props[DefaultController])
    val interface = system.actorOf(Props[CommandLineInterface])
    val pluginManager = system.actorOf(Props[DefaultPluginManager])
    val resourceManager = system.actorOf(Props[DefaultResourceManager])

    //setup subscriptions
    EventBus.subscribe(controller, ControllerConstant.channel)
    EventBus.subscribe(interface, UserInterfaceConstant.channel)
    EventBus.subscribe(pluginManager,PluginManagerConstant.channel)
    EventBus.subscribe(resourceManager, ResourceManagerConstant.channel)

    //initialisation
    EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.setup, None)))
  }

}
