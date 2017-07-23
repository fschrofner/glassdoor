package io.glassdoor.plugin.plugins.preprocessor.java

import java.io.File

import io.glassdoor.application._
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

        val arguments = new JadxArgs

        val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

        if(parameterArray.isDefined) {
          for(parameter <- parameterArray.get){
            if(parameter.paramType == ParameterType.Flag){
              parameter.name match {
                case "deobfuscate" | "d" =>
                  arguments.setDeobfuscationOn(true)
              }
            }
          }
        }

        Log.debug("decompiling " + apkPath.get)
        showEndlessProgress()

        val decompiler = new JadxDecompiler(arguments)
        val file = new File(apkPath.get)
        val outputDirPath = workingDir.get + File.separator + ContextConstant.Key.Java
        val outputDir = new File(outputDirPath)

        decompiler.setOutputDir(outputDir)
        decompiler.loadFile(file)
        decompiler.saveSources()

        val result = HashMap[String,String](ContextConstant.FullKey.IntermediateSourceJava -> outputDirPath)
        mResult = Some(result)
      } else {
        setErrorMessage("error: apk path not defined")
      }
    } else {
      setErrorMessage("error: working directory not defined")
    }

    ready()
  }

  override def result: Option[Map[String, String]] = {
    mResult
  }

}
