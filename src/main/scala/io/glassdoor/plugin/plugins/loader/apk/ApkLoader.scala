package io.glassdoor.plugin.plugins.loader.apk

import java.io.{IOException, File}
import java.nio.file.{CopyOption, Path}
import java.nio.file.Paths.get
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import io.glassdoor.application.{Log, ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import java.nio.file.Files.copy

import scala.collection.immutable.HashMap

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class ApkLoader extends Plugin{
  var mResult:Option[Map[String,String]] = None

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    if(parameters.length > 0){
      val srcPath = parameters(0)
      val srcFile = new File(srcPath)

      if(srcFile.exists()){
        val path = copyApkToWorkingDirectory(data, srcPath)
        if(path.isDefined){
          val result = HashMap[String,String](ContextConstant.FullKey.OriginalBinaryApk -> path.get)
          mResult = Some(result)
        } else {
          //TODO: error handling
          setErrorMessage("error: working dir is not defined or error while copying!")
          mResult = None
        }
      } else {
        setErrorMessage("error: source file does not exist!")
      }

    } else {
      setErrorMessage("error: not enough parameters")
    }

    ready()
  }

  override def result: Option[Map[String,String]] = {
    mResult
  }

  def copyApkToWorkingDirectory(data:Map[String,String], srcPath:String): Option[String] ={
    val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

    if(workingDir.isDefined){
      try {
        val destPath = workingDir.get + "/" + ContextConstant.Key.Apk + "/" + getFileName(srcPath)
        createFolderStructure(destPath)
        Log.debug("copying apk to: " + destPath + "...")
        copy(get(srcPath),get(destPath), REPLACE_EXISTING)
        return Some(destPath)
      } catch {
        case e:IOException =>
          return None
      }
    } else {
      Log.debug("working directory not defined!")
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

}
