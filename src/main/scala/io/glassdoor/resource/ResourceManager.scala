package io.glassdoor.plugin.resource

import akka.actor.Actor
import io.glassdoor.bus.Message
import io.glassdoor.resource.Resource
import io.glassdoor.application._

/**
  * Created by Florian Schrofner on 4/15/16.
  */
trait ResourceManager extends Actor {
  def installResource(name:String, context:Context):Unit
  def getResource(name:String):Option[Resource]
  def loadResources(context:Context):Unit

  override def receive = {
    case Message(action, data) =>
      action match {
        case ResourceManagerConstant.Action.buildResourceIndex =>
          if(data.isDefined){
            loadResources(data.get.asInstanceOf[Context])
          }
        case ResourceManagerConstant.Action.installResource =>
          if(data.isDefined){
            val parameters = data.get.asInstanceOf[ResourceManagerResourceParameters]
            installResource(parameters.resourceName, parameters.context)
          }
      }
  }
}

object ResourceManagerConstant {
  val channel = "/resourceManager"

  object Action {
    val buildResourceIndex = "buildPluginIndex"
    val installResource = "installResource"
  }

  object Path {
    val RESOURCE_REPOSITORY_DIRECTORY = "repositories"
  }
}

case class ResourceManagerResourceParameters(resourceName:String, context:Context)
