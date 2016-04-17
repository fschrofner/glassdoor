package io.glassdoor.plugin

import akka.actor.Actor
import io.glassdoor.bus.Message

/**
  * Created by Florian Schrofner on 4/15/16.
  */
trait ResourceManager extends Actor {
  def receive = {
    case Message(action, data) => println(action)
  }
}
