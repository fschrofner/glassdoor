package io.glassdoor.plugin.resource

import akka.actor.Actor
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.resource.Resource
import io.glassdoor.application._
import io.glassdoor.controller.ControllerConstant

/**
  * Created by Florian Schrofner on 4/15/16.
  */
trait ResourceManager extends Actor {
  def installResource(name:String, context:Context):Unit
  def removeResource(name:String, context:Context):Unit
  def handleResourceInstallCallback(keymap:Map[String,String])
  def getResource(name:String):Option[Resource]
  def initialise(context:Context):Unit
  def updateAvailableResourceIndex(context:Context):Unit

  def sendErrorMessage(resource:Option[Resource], errorCode:Integer, data:Option[Any]):Unit = {
    val messageData = new ResourceErrorMessage(resource, errorCode, data)
    val message = new Message(ControllerConstant.Action.ResourceError, Some(messageData))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  def sendSuccessMessage(resource:Option[Resource], code:Integer):Unit = {
    val messageData = new ResourceSuccessMessage(resource, code)
    val message = new Message(ControllerConstant.Action.ResourceSuccess, Some(messageData))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  def addResourcesToContext(changedValues:Map[String,String]):Unit = {
    val message = new Message(ControllerConstant.Action.ApplyChangedValues, Some(changedValues))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  def removeResourcesFromContext(removedValues:Array[String]):Unit = {
    val message = new Message(ControllerConstant.Action.ApplyRemovedValues, Some(removedValues))
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
  }

  override def receive = {
    case Message(action, data) =>
      action match {
        case ResourceManagerConstant.Action.Initialise =>
          if(data.isDefined){
            initialise(data.get.asInstanceOf[Context])
          }
        case ResourceManagerConstant.Action.InstallResource =>
          if(data.isDefined){
            val parameters = data.get.asInstanceOf[ResourceManagerResourceParameters]
            installResource(parameters.resourceName, parameters.context)
          }
        case ResourceManagerConstant.Action.RemoveResource =>
          if(data.isDefined){
            val parameters = data.get.asInstanceOf[ResourceManagerResourceParameters]
            removeResource(parameters.resourceName, parameters.context)
          }
        case ResourceManagerConstant.Action.UpdateAvailableResourceIndex =>
          if(data.isDefined){
            updateAvailableResourceIndex(data.get.asInstanceOf[Context])
          }
        case ResourceManagerConstant.Action.ResourceInstallComplete =>
          if(data.isDefined){
            //TODO: also catch errors during resource install
            val changedResource = data.get.asInstanceOf[Map[String,String]]
            handleResourceInstallCallback(changedResource)
          }
      }
  }
}

object ResourceManagerConstant {
  val Channel = "/resourceManager"

  object Action {
    val Initialise = "initialise"
    val InstallResource = "installResource"
    val RemoveResource = "removeResource"
    val UpdateAvailableResourceIndex = "updateAvailableResourceIndex"
    val ResourceInstallComplete = "resourceInstallComplete"
  }

  object ResourceSuccessCode {
    val ResourceSuccessfullyInstalled = 100
    val ResourceSuccessfullyRemoved = 101
  }
  object ResourceErrorCode {
    val ResourceAlreadyInstalled = 100
    val ResourceNotFound = 101
  }
}

case class ResourceManagerResourceParameters(resourceName:String, context:Context)
case class ResourceErrorMessage(resource:Option[Resource], errorCode:Integer, data:Option[Any])
case class ResourceSuccessMessage(resource:Option[Resource], code:Integer)
