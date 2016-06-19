package io.glassdoor.plugin.plugins.preprocessor.java

import java.io.File

import io.glassdoor.application.{Log, ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import jadx.api._

import scala.collection.immutable.HashMap

class JavaDecompiler extends Plugin {
  var mResult:Option[Map[String,String]] = None

  override def apply(data: Map[String,String], parameters: Array[String]): Unit = {
    val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(workingDir.isDefined){
      val apkPath = data.get(ContextConstant.FullKey.OriginalBinaryApk)

      if(apkPath.isDefined){
        Log.debug("decompiling " + apkPath.get)
        showEndlessProgress()

        val decompiler = new JadxDecompiler()
        val file = new File(apkPath.get)
        val outputDirPath = workingDir.get + "/" + ContextConstant.Key.Java
        val outputDir = new File(outputDirPath)

        decompiler.setOutputDir(outputDir)
        decompiler.loadFile(file)
        decompiler.saveSources()

        val result = HashMap[String,String](ContextConstant.FullKey.IntermediateSourceJava -> outputDirPath)
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
