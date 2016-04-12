package io.glassdoor.plugin.plugins.preprocessor.smali

import java.io.File

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin
import org.jf.baksmali.{baksmaliOptions, baksmali}
import org.jf.dexlib2.{DexFileFactory, Opcodes}
import org.jf.dexlib2.iface.{ClassDef, DexFile}

/**
  * Created by Florian Schrofner on 3/16/16.
  */
class SmaliDisassembler extends Plugin{
  var mContext:Context = _

  override def apply(context: Context, parameters: Array[String]): Unit = {
    //baksmali.disassembleDexFile(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX))
    //val folder = new File(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX))
    mContext = context

    val workingDir = mContext.getResolvedValue(Constant.Context.FullKey.CONFIG_WORKING_DIRECTORY)

    if(workingDir.isDefined){
      val outputDirectory = workingDir.get + "/" + Constant.Context.Key.SMALI

      val options = new baksmaliOptions
      options.jobs = 1
      options.outputDirectory = outputDirectory

      val destination = context.getResolvedValue(Constant.Context.FullKey.CONFIG_WORKING_DIRECTORY)

      //TODO: use destination

      val dexFilePath = context.getResolvedValue(Constant.Context.FullKey.INTERMEDIATE_ASSEMBLY_DEX)

      if(dexFilePath.isDefined){
        val dexFileFile = new File(dexFilePath.get + "/classes.dex")
        val dexFile = DexFileFactory.loadDexFile(dexFileFile, options.dexEntry, options.apiLevel, options.experimental);

        try {
          baksmali.disassembleDexFile(dexFile, options)
          println("disassembling dex to: " + outputDirectory)
          mContext.intermediateAssembly += ((Constant.Context.Key.SMALI, outputDirectory))
        } catch {
          case e:IllegalArgumentException =>
            println("illegal argument exception")
            e.printStackTrace()
        }
      } else {
        println("dex not defined!")
      }
    }


  }

  override def result: Context = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
