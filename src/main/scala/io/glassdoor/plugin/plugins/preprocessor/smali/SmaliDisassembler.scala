package io.glassdoor.plugin.plugins.preprocessor.smali

import java.io.File

import io.glassdoor.application.{Log, ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import org.jf.baksmali.{baksmaliOptions, baksmali}
import org.jf.dexlib2.{DexFileFactory, Opcodes}
import org.jf.dexlib2.iface.{ClassDef, DexFile}

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/16/16.
  */
class SmaliDisassembler extends Plugin{
  var mResult:Option[Map[String,String]] = None

  override def apply(data: Map[String,String], parameters: Array[String]): Unit = {
    val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(workingDir.isDefined){
      val outputDirectory = workingDir.get + "/" + ContextConstant.Key.Smali

      val options = new baksmaliOptions
      options.jobs = 1
      options.outputDirectory = outputDirectory

      val dexFilePath = data.get(ContextConstant.FullKey.IntermediateAssemblyDex)

      if(dexFilePath.isDefined){
        val dexFileFile = new File(dexFilePath.get + "/classes.dex")
        val dexFile = DexFileFactory.loadDexFile(dexFileFile, options.dexEntry, options.apiLevel, options.experimental);

        try {
          showEndlessProgress()
          baksmali.disassembleDexFile(dexFile, options)
          Log.debug("disassembling dex to: " + outputDirectory)
          val result = HashMap[String,String](ContextConstant.FullKey.IntermediateAssemblySmali -> outputDirectory)
          mResult = Some(result)
        } catch {
          case e:IllegalArgumentException =>
            mResult = None
        }
      } else {
        setErrorMessage("error: dex not defined!")
      }
    } else {
      setErrorMessage("error: working directory not defined")
    }

    ready()
  }

  override def result:Option[Map[String,String]] = {
    mResult
  }

}
