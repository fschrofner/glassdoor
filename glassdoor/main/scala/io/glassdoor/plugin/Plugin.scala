package io.glassdoor.plugin

import io.glassdoor.application.Context

/**
  * Created by Florian Schrofner on 3/16/16.
  */
trait Plugin {
  def apply(context: Context, parameters:Array[String])
  def result:Context
  def help(parameters:Array[String])
}
