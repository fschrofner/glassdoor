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
  var mContext:Option[Context] = None

  override def apply(context: Context, parameters: Array[String]): Unit = {
    try {
      val regex = parameters(0)
      val src = parameters(1)
      val dest = parameters(2)
      callGrep(regex,src,dest, context)
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        mContext = None
    }

  }

  def callGrep(regex:String, src:String, dest:String, context:Context): Unit ={
    val srcPath = context.getResolvedValue(src)
    val workingDirectory = context.getResolvedValue(Constant.Context.FullKey.CONFIG_WORKING_DIRECTORY)

    if(srcPath.isDefined && workingDirectory.isDefined){
      val destPath = workingDirectory + context.splitDescriptor(dest)(1) + "/result.log"
      val outputFile = new File(destPath)
      outputFile.getParentFile.mkdirs()

      val command = "grep -rohE \"" + regex + "\" " + srcPath.get
      val output = command.!!

      //write the resulting log
      val bw = new BufferedWriter(new FileWriter(outputFile))
      bw.write(output)
      bw.close()

      //TODO: the path to the exact file should not be saved in context, but only the directory
      context.setResolvedValue(dest, outputFile.getParent)
      mContext = Some(context)
    }

  }

  override def result: Option[Context] = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
