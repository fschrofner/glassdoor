package io.glassdoor.plugin.plugins.preprocessor.extractor

import java.io.{OutputStream, FileOutputStream, InputStream, File}
import java.util.zip.{ZipEntry, ZipFile}
import scala.collection.JavaConversions._

import io.glassdoor.application.{Log, ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.util.matching.Regex

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class Extractor extends Plugin{
  var mResult:Option[Map[String,String]] = None
  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {

    try {
      //get source
      val srcKeymapDescription = parameters(1)
      val srcPath = data.get(srcKeymapDescription)

      //determine destination
      val keymapDescription = parameters(2)
      val keymapSplitString = splitDescriptor(keymapDescription)

      val keymapName = keymapSplitString(0)
      val keyValue = keymapSplitString(1)

      val workingDir = data.get(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY)

      if(srcPath.isDefined && workingDir.isDefined){
        val destination = workingDir.get + "/" + keyValue

        val regex = parameters(0)
        //TODO: check regex for null

        //TODO: get keymap from parameters + destination dir
        extract(srcPath.get, regex, destination)

        val result = HashMap[String,String](keymapDescription -> destination)
        mResult = Some(result)
      } else {
        //TODO: error handling when working dir is not defined
        mResult = None
      }
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        mResult = None
    } finally {
      Log.debug("extractor ready")
      ready
    }


  }

  def extract(source: String, regex: String, targetFolder: String) = {
    val sourceFile = new ZipFile(source)

    val entryList = sourceFile.entries().toList
    val regexObject = regex.r

    Log.debug("extracting files to: " + targetFolder)

    for (entry <- entryList) {
      entry.getName match {
        case regexObject() =>
          new File(targetFolder, entry.getName).getParentFile.mkdirs()
          saveFile(sourceFile.getInputStream(entry), new FileOutputStream(new File(targetFolder, entry.getName)))
        case _ =>

      }
    }
  }


  def saveFile(fis: InputStream, fos: OutputStream) = {
    writeToFile(bufferReader(fis)_, fos)
    fis.close
    fos.close
  }

  def bufferReader(fis: InputStream)(buffer: Array[Byte]) = (fis.read(buffer), buffer)

  def writeToFile(reader: (Array[Byte]) => Tuple2[Int, Array[Byte]], fos: OutputStream): Unit = {
    val (length, data) = reader(buffer)
    if (length >= 0) {
      fos.write(data, 0, length)
      writeToFile(reader, fos)
    }
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DESCRIPTOR_SPLIT_REGEX)
  }

  override def result:Option[Map[String,String]] = {
    mResult
  }

  override def help(parameters: Array[String]): Unit = ???
}
