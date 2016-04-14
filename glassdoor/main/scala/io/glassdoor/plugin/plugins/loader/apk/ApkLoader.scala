package io.glassdoor.plugin.plugins.loader.apk

import java.io.{IOException, File}
import java.nio.file.{CopyOption, Path}
import java.nio.file.Paths.get
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

import io.glassdoor.application.{Constant, Context}
import io.glassdoor.plugin.Plugin
import java.nio.file.Files.copy

/**
  * Created by Florian Schrofner on 3/17/16.
  */
class ApkLoader extends Plugin{
  var mContext:Option[Context] = None

  override def apply(context: Context, parameters: Array[String]): Unit = {
    val srcPath = parameters(0)

    //TODO: match against regex to check if url or local path
    //TODO: make a copy to a working directory of glassdoor
    //TODO: make plugin trait more powerful = check if command exists/show error otherwise

    val path = copyApkToWorkingDirectory(context, srcPath)
    if(path.isDefined){
      context.setResolvedValue(Constant.Context.FullKey.ORIGINAL_BINARY_APK, path.get)
      mContext = Some(context)
    } else {
      //TODO: error handling
      mContext = None
    }
  }

  override def result: Option[Context] = {
    mContext
  }

  def copyApkToWorkingDirectory(context:Context, srcPath:String): Option[String] ={
    val workingDir = context.getResolvedValue(Constant.Context.FullKey.CONFIG_WORKING_DIRECTORY)
    if(workingDir.isDefined){
      try {
        val destPath = workingDir.get + "/" + Constant.Context.Key.APK + "/" + getFileName(srcPath)
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
