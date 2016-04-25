package io.glassdoor.plugin

import akka.actor.ActorRef

/**
  * Created by Florian Schrofner on 4/5/16.
  */
case class PluginInstance(name:String, kind:String, dependencies:Array[String], changes:Array[String], commands:Array[String], plugin:ActorRef)
