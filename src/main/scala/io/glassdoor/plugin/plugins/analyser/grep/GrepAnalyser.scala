package io.glassdoor.plugin.plugins.analyser.grep

import java.io.{BufferedWriter, FileWriter, File}
import java.rmi.activation.ActivationGroup_Stub

import io.glassdoor.application.{ContextConstant, Context, Constant}
import io.glassdoor.plugin.Plugin
import scala.collection.immutable.HashMap
import scala.sys.process._

/**
  * Runs grep with the specified regex over the files inside the specified location.
  * Stores the matching lines in a log file in the specified destination.
  * Created by Florian Schrofner on 3/30/16.
  */
class GrepAnalyser extends Plugin{

  var mResult:Option[Map[String,String]] = None

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    try {
      val regex = parameters(0)
      val src = parameters(1)
      val dest = parameters(2)
      callGrep(regex,src,dest, data)
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        mResult = None
    } finally {
      ready
    }

  }

  def callGrep(regex:String, src:String, dest:String, data:Map[String,String]): Unit ={
    val srcPath = data.get(src)
    val workingDirectory = data.get(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY)

    if(srcPath.isDefined && workingDirectory.isDefined){
      val destPath = workingDirectory.get + "/" + ContextConstant.Key.GREP + "/" + splitDescriptor(dest)(1) + "/result.log"
      val outputFile = new File(destPath)
      outputFile.getParentFile.mkdirs()

      val command = "grep -rohE \"" + regex + "\" " + srcPath.get
      val output = command.!!

      //write the resulting log
      val bw = new BufferedWriter(new FileWriter(outputFile))
      bw.write(output)
      bw.close()

      println("grep finished, saved log to: " + outputFile.getAbsolutePath)

      //TODO: the path to the exact file should not be saved in context, but only the directory
      val result = HashMap[String,String](dest -> outputFile.getParent)
      mResult = Some(result)
    }

  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DESCRIPTOR_SPLIT_REGEX)
  }

  override def result: Option[Map[String,String]] = {
    mResult
  }

  override def help(parameters: Array[String]): Unit = ???
}
