package io.glassdoor.plugin.plugins.meta.show

import io.glassdoor.application.Log
import io.glassdoor.plugin.{DynamicValues, Plugin}

/**
  * Only prints the given value in the user interface
  * Created by Florian Schrofner on 7/4/16.
  */
class ShowValue extends Plugin {
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    Log.debug("apply show called")

    if(parameters.length > 0){
      val value = data.get(parameters(0))
      if(value.isDefined){
        printInUserInterface(value.get)
      } else {
        setErrorMessage("error: value unknown")
      }
    } else {
      setErrorMessage("error: no value given")
    }
    ready()
  }


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if(parameters.length > 0){
      Log.debug("more than 0 parameters specified")
      DynamicValues(uniqueId, Some(Array(parameters(0))),None)
    } else {
      Log.debug("no parameters specified")
      DynamicValues(uniqueId, Some(Array[String]()), None)
    }
  }

  override def result: Option[Map[String, String]] = {
    Some(Map[String,String]())
  }

}
