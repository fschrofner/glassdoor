package io.glassdoor.plugin.plugins.loader.apk

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class ApkLoader extends Plugin{
  var mContext:Context = _

  override def apply(context: Context, parameters: Array[String]): Unit = {
    mContext = context
    val path = parameters(0)
    //TODO: match against regex to check if url or local path
    //TODO: make a copy to a working directory of glassdoor
    //TODO: make plugin trait more powerful = check if command exists/show error otherwise
    mContext.originalBinary += ((Constant.ORIGINAL_BINARY_APK,path))
  }

  override def result: Context = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
