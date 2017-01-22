package io.glassdoor.plugin.plugins.emulator.adb

import io.glassdoor.plugin.Plugin

/**
  * Created by Florian Schrofner on 1/22/17.
  */
class Adb extends Plugin {
  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {

  }

  /**
    * This will be called once you call the ready() method inside your plugin.
    * Please return ALL the changed values here as a map containing the key and the changed value.
    * If you did not change any values, simply return an empty map = Some(Map[String,String]())
    * Returning None here, will be interpreted as an error.
    *
    * @return a map containing all the changed values.
    */
override def result: Option[Map[String, String]] = ???

  override def help(parameters: Array[String]): Unit = ???
}
