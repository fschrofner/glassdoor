package io.glassdoor.plugin.plugins.preprocessor.extractor

import java.io.{OutputStream, FileOutputStream, InputStream, File}
import java.util.zip.{ZipEntry, ZipFile}
import scala.collection.JavaConversions._

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin

import scala.collection.immutable.HashMap
import scala.util.matching.Regex

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class Extractor extends Plugin{
  var mContext:Context = _
  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)

  override def apply(context: Context, parameters: Array[String]): Unit = {
    mContext = context

    //determine destination
    val keymapDescription = parameters(1)
    val keymapSplitString = mContext.splitDescriptor(keymapDescription)

    val keymapName = keymapSplitString(0)
    val keyValue = keymapSplitString(1)

    val apkPath = context.getResolvedValue(Constant.Context.FullKey.ORIGINAL_BINARY_APK)

    //TODO: load targetfolder from config

    val workingDir = context.getResolvedValue(Constant.Context.FullKey.CONFIG_WORKING_DIRECTORY)

    if(apkPath.isDefined && workingDir.isDefined){
      val destination = workingDir.get + "/" + keyValue

      val regex = parameters(0)
      //TODO: check regex for null

      //TODO: get keymap from parameters + destination dir
      extract(apkPath.get, regex, destination)

      mContext.setResolvedValue(keymapDescription,destination)
    } else {
      //TODO: error handling when working dir is not defined
    }

  }

  def extract(source: String, regex: String, targetFolder: String) = {
    val sourceFile = new ZipFile(source)

    val entryList = sourceFile.entries().toList
    val regexObject = regex.r

    println("extracting files to: " + targetFolder)

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

  override def result: Context = {
    mContext
  }

  override def help(parameters: Array[String]): Unit = ???
}
