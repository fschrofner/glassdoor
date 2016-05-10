package io.glassdoor.plugin.resource

import akka.actor.Actor
import io.glassdoor.bus.Message
import io.glassdoor.resource.Resource
import io.glassdoor.application._

/**
  * Created by Florian Schrofner on 4/15/16.
  */
trait ResourceManager extends Actor {
  def installResource(name:String):Unit
  def getResource(name:String):Option[Resource]
  def loadResources(context:Context):Unit

  def receive = {
    case Message(action, data) => println(action)
  }
}

