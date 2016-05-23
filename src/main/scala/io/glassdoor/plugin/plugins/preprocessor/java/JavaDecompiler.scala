package io.glassdoor.plugin.plugins.preprocessor.java

import java.io.File

import io.glassdoor.application.{ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import jadx.api._

import scala.collection.immutable.HashMap

class JavaDecompiler extends Plugin {
  var mResult:Option[Map[String,String]] = None

  override def apply(data: Map[String,String], parameters: Array[String]): Unit = {
    val workingDir = data.get(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY)

    if(workingDir.isDefined){
      val apkPath = data.get(ContextConstant.FullKey.ORIGINAL_BINARY_APK)

      if(apkPath.isDefined){
        println("decompiling " + apkPath.get)

        val decompiler = new JadxDecompiler()
        val file = new File(apkPath.get)
        val outputDirPath = workingDir.get + "/" + ContextConstant.Key.JAVA
        val outputDir = new File(outputDirPath)

        decompiler.setOutputDir(outputDir)
        decompiler.loadFile(file)
        decompiler.saveSources()
        //decompiler.parse()

        val result = HashMap[String,String](ContextConstant.FullKey.INTERMEDIATE_SOURCE_JAVA -> outputDirPath)
        mResult = Some(result)
      }
    }

    ready
  }

  override def result: Option[Map[String, String]] = {
    mResult
  }

  override def help(parameters: Array[String]): Unit = ???
}
