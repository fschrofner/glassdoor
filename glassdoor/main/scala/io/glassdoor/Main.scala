package io.glassdoor

import akka.actor.{Props, ActorSystem}
import io.glassdoor.application.{ContextConstant, Configuration, Constant, Context}
import io.glassdoor.bus.{MessageEvent, Message, EventBus}
import io.glassdoor.controller.{DefaultController, ControllerConstant, Controller}
import io.glassdoor.interface.{UserInterfaceConstant, UserInterface, CommandLineInterface}
import io.glassdoor.plugin.{PluginManagerConstant, DefaultResourceManager, DefaultPluginManager, Plugin}
import io.glassdoor.plugin.plugins.analyser.grep.GrepAnalyser
import io.glassdoor.plugin.plugins.loader.apk.ApkLoader
import io.glassdoor.plugin.plugins.preprocessor.extractor.Extractor
import io.glassdoor.plugin.plugins.preprocessor.smali.SmaliDisassembler

object Main {

  def main(args:Array[String]):Unit={
    val system = ActorSystem()

    println("the first line of glassdoor!")

    //create components
    val controller = system.actorOf(Props(new DefaultController))
    val interface = system.actorOf(Props(new CommandLineInterface))
    val pluginManager = system.actorOf(Props(new DefaultPluginManager))
    //val resourceManager = system.actorOf(Props(new DefaultResourceManager))

    //setup subscriptions
    EventBus.subscribe(controller, ControllerConstant.channel)
    EventBus.subscribe(interface, UserInterfaceConstant.channel)
    EventBus.subscribe(pluginManager,PluginManagerConstant.channel)
    //EventBus.subscribe(resourceManager, "/resourceManager")


    //initialisation
    EventBus.publish(MessageEvent(ControllerConstant.channel, Message(ControllerConstant.Action.setup, None)))
  }

}
