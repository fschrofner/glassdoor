package io.glassdoor.bus

import akka.actor.ActorRef
import akka.event.{LookupClassification, ActorEventBus}
import io.glassdoor.application.Log

/**
  * Created by Florian Schrofner on 4/17/16.
  */

case class MessageEvent(val channel: String, val message: Message)
case class Message(action: String, data:Option[Any])

object EventBus extends ActorEventBus with LookupClassification {

  override type Classifier = String
  override type Event = MessageEvent

  override protected def mapSize(): Int = 10

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.message
    Log.debug("eventbus: sent message: " + event.channel + ":" + event.message.action)
  }

  override protected def classify(event: Event): Classifier = {
    event.channel
  }


}
