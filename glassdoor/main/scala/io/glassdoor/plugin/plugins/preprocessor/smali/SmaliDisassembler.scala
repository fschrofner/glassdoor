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

    val outputDirectory = Constant.ROOT_DIRECTORY + Constant.INTERMEDIATE_ASSEMBLY_SMALI

    val options = new baksmaliOptions
    options.jobs = 1
    options.outputDirectory = outputDirectory

    val dexFileFile = new File(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX)+"/classes.dex")
    val dexFile = DexFileFactory.loadDexFile(dexFileFile, options.dexEntry, options.apiLevel, options.experimental);

    try {
      baksmali.disassembleDexFile(dexFile, options)
      mContext.intermediateAssembly += ((Constant.INTERMEDIATE_ASSEMBLY_SMALI,outputDirectory))
    } catch {
      case e:IllegalArgumentException =>
        println("illegal argument exception")
        e.printStackTrace()
    }
  }

  override def result: Context = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
