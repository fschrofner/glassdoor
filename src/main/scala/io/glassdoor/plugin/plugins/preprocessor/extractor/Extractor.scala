package io.glassdoor.plugin.plugins.preprocessor.extractor

import java.io.{File, FileOutputStream, InputStream, OutputStream}
import java.util.zip.{ZipEntry, ZipFile}

import scala.collection.JavaConversions._
import io.glassdoor.application.{Constant, Context, ContextConstant, Log}
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.util.matching.Regex

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class Extractor extends Plugin{
  var mResult:Option[Map[String,String]] = None
  val BUFSIZE = 4096
  val buffer = new Array[Byte](BUFSIZE)


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if(parameters.length > 0){
      DynamicValues(uniqueId, Some(Array[String](parameters(1))), Some(Array[String](parameters(2))))
    } else {
      DynamicValues(uniqueId, Some(Array[String]()), Some(Array[String]()))
    }
  }

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    Log.debug("apply extractor called, size of parameters: " + parameters.length)

    try {
      //get source
      val srcKeymapDescription = parameters(1)
      val srcPath = data.get(srcKeymapDescription)

      //determine destination
      val keymapDescription = parameters(2)
      val keymapSplitString = splitDescriptor(keymapDescription)

      val keymapName = keymapSplitString(0)
      val keyValue = keymapSplitString(1)

      val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

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
        setErrorMessage("error: working directory not defined")
        mResult = None
      }
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        Log.debug("array index out of bounds, not enough arguments")
        setErrorMessage("error: not enough parameters")
        mResult = None
    } finally {
      Log.debug("extractor ready")
      ready()
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
    descriptor.split(Constant.Regex.DescriptorSplitRegex)
  }

  override def result:Option[Map[String,String]] = {
    mResult
  }

}
