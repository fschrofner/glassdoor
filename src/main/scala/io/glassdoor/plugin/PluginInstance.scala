package io.glassdoor.plugin

import akka.actor.ActorRef

/**
  * Case class containing all the data of an instance of a running plugin.
  * Created by Florian Schrofner on 4/5/16.
  */
case class PluginInstance(uniqueId: Long, name:String, kind:String, dependencies:Array[String], changes:Array[String], commands:Array[String], plugin:ActorRef)
