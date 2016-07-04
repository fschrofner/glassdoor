package io.glassdoor.plugin

import akka.actor.Actor
import io.glassdoor.application.Context
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.interface.UserInterfaceConstant
import io.glassdoor.plugin.manager.PluginManagerConstant

/**
  * Trait which needs to be extended by every plugin.
  * The actual implementation of the plugin is done here.
  * Created by Florian Schrofner on 3/16/16.
  */
trait Plugin extends Actor {
  var uniqueId:Option[Long] = None

  /**
    * This is the method called, when your plugin gets launched.
    * @param data a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  def apply(data:Map[String,String], parameters:Array[String])

  /**
    * This will be called once you call the ready() method inside your plugin.
    * Please return ALL the changed values here as a map containing the key and the changed value.
    * If you did not change any values, simply return an empty map = Some(Map[String,String]())
    * Returning None here, will be interpreted as an error.
    * @return a map containing all the changed values.
    */
  def result:Option[Map[String,String]]
  def help(parameters:Array[String])

  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  def resolveDynamicValues(parameters:Array[String]): DynamicValues = {
    return DynamicValues(uniqueId,None,None)
  }

  //method should be called when the plugin is ready to return a result
  def ready():Unit = {
    val resultData = PluginResult(uniqueId, result)
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginTaskCompleted, uniqueId)))
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginResult, Some(resultData))))
  }

  //updates the progress of this task to the specified value
  def showProgress(progress:Double):Unit = {
    //TODO
  }

  def showEndlessProgress():Unit = {
    EventBus.publish(new MessageEvent(PluginManagerConstant.Channel, Message(PluginManagerConstant.Action.PluginShowEndlessProgress, uniqueId)))
  }

  def printInUserInterface(message:String):Unit = {
    EventBus.publish(new MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.PrintInUi, Some(message))))
  }

  override def receive: Receive = {
    case Message(action, data) =>
      action match {
        case PluginConstant.Action.Apply =>
          if(data.isDefined){
            val pluginParameters = data.get.asInstanceOf[PluginParameters]
            apply(pluginParameters.data, pluginParameters.parameters)
          }
        case PluginConstant.Action.SetUniqueId =>
          if(data.isDefined){
            val id = data.get.asInstanceOf[Long]
            uniqueId = Some(id)
          }
        case PluginConstant.Action.ResolveDynamicValues =>
          if(data.isDefined){
            val parameters = data.get.asInstanceOf[Array[String]]
            val messageData = resolveDynamicValues(parameters)
            val message = Message(PluginManagerConstant.Action.DynamicValueUpdate, Some(messageData))
            EventBus.publish(MessageEvent(PluginManagerConstant.Channel, message))
          }
      }
  }
}

object PluginConstant {
  object Action {
    val Apply = "apply"
    val Help = "help"
    val SetUniqueId = "id"
    val ResolveDynamicValues = "resolveDynamicValues"
  }
}

case class PluginParameters(data:Map[String, String], parameters:Array[String])
case class PluginResult(uniqueId:Option[Long], result:Option[Map[String,String]])
case class PluginProgress(uniqueId:Option[Long], pluginName:String, progress:Option[Integer])
case class DynamicValues(uniqueId:Option[Long], dependencies:Option[Array[String]], changes:Option[Array[String]])
