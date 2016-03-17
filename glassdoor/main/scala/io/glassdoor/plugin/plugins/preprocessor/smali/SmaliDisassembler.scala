package io.glassdoor.plugin.plugins.preprocessor.smali

import java.io.File
import java.util

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin
import org.jf.baksmali.{baksmaliOptions, baksmali}
import org.jf.dexlib2.{DexFileFactory, Opcodes}
import org.jf.dexlib2.iface.{ClassDef, DexFile}

/**
  * Created by Florian Schrofner on 3/16/16.
  */
class SmaliDisassembler extends Plugin{
  override def apply(context: Context, parameters: Array[String]): Unit = {
    //baksmali.disassembleDexFile(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX))
    //val folder = new File(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX))
    val file = DexFileFactory.loadDexFile(context.intermediateAssembly(Constant.INTERMEDIATE_ASSEMBLY_DEX)+"/classes.dex",2)
    baksmali.disassembleDexFile(file, new baksmaliOptions)
  }

  override def result: Context = ???

  override def help(parameters: Array[String]): Unit = ???
}
