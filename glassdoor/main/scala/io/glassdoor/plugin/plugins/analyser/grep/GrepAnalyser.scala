package io.glassdoor.plugin.plugins.analyser.grep

import java.io.{BufferedWriter, FileWriter, File}
import java.rmi.activation.ActivationGroup_Stub

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin
import scala.sys.process._

/**
  * Runs grep with the specified regex over the files inside the specified location.
  * Stores the matching lines in a log file in the specified destination.
  * Created by Florian Schrofner on 3/30/16.
  */
class GrepAnalyser extends Plugin{
  var mContext:Context = null

  override def apply(context: Context, parameters: Array[String]): Unit = {
    mContext = context

    //TODO: check if parameters have correct size
    val regex = parameters(0)
    val src = parameters(1)
    val dest = parameters(2)

    callGrep(regex,src,dest)
  }

  def callGrep(regex:String, src:String, dest:String): Unit ={
    val srcPath = mContext.getResolvedValue(src)

    if(srcPath.isDefined){
      val destPath = Constant.ROOT_DIRECTORY + mContext.splitDescriptor(dest)(1) + "/result.log"
      val outputFile = new File(destPath)
      outputFile.getParentFile.mkdirs()

      val command = "grep -rohE \"" + regex + "\" " + srcPath.get
      val output = command.!!

      //write the resulting log
      val bw = new BufferedWriter(new FileWriter(outputFile))
      bw.write(output)
      bw.close()

      //TODO: the path to the exact file should not be saved in context, but only the directory
      mContext.setResolvedValue(dest, outputFile.getParent)
    }

  }

  override def result: Context = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
