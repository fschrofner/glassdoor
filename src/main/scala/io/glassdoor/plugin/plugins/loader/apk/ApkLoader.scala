package io.glassdoor.plugin.plugins.loader.apk

import java.io.{IOException, File}
import java.nio.file.{CopyOption, Path}
import java.nio.file.Paths.get
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import io.glassdoor.application.{ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import java.nio.file.Files.copy

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class ApkLoader extends Plugin{
  var mResult:Option[Map[String,String]] = None

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    val srcPath = parameters(0)

    //TODO: match against regex to check if url or local path
    //TODO: make a copy to a working directory of glassdoor
    //TODO: make plugin trait more powerful = check if command exists/show error otherwise

    val path = copyApkToWorkingDirectory(data, srcPath)
    if(path.isDefined){
      val result = HashMap[String,String](ContextConstant.FullKey.ORIGINAL_BINARY_APK -> path.get)
      mResult = Some(result)
    } else {
      //TODO: error handling
      mResult = None
    }

    ready()
  }

  override def result: Option[Map[String,String]] = {
    mResult
  }

  def copyApkToWorkingDirectory(data:Map[String,String], srcPath:String): Option[String] ={
    val workingDir = data.get(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY)
    if(workingDir.isDefined){
      try {
        val destPath = workingDir.get + "/" + ContextConstant.Key.APK + "/" + getFileName(srcPath)
        createFolderStructure(destPath)
        println("copying apk to: " + destPath + "...")
        copy(get(srcPath),get(destPath), REPLACE_EXISTING)
        return Some(destPath)
      } catch {
        case e:IOException =>
          return None
      }
    } else {
      println("working directory not defined!")
    }
    return None
  }

  def createFolderStructure(path:String):Unit = {
    val file = new File(path)
    file.getParentFile.mkdirs()
  }

  def getFileName(path:String):String = {
    val file = new File(path)
    return file.getName
  }

  override def help(parameters: Array[String]): Unit = ???
}
