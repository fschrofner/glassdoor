package io.glassdoor.plugin.plugins.installer.git

import java.io.{IOException, File}
import java.nio.file.{Files, Path}

import io.glassdoor.application.{ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import scala.collection.immutable.HashMap
import scala.sys.process._

/**
  * A plugin used to download and update data from Git repositories.
  * Could be used to update/receive dictionaries or other plugins.
  * Created by Florian Schrofner on 4/15/16.
  */
class GitInstaller extends Plugin {

  var mResult:Option[Map[String,String]] = None

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    try {
      val repoUrl = parameters(0)
      val path = parameters(1)
      var keymap:Option[String] = None
      var resultCode = 1

      //TODO: if the gitupdater is used to install a plugin, it might be needed to save it somewhere
      if(parameters.length > 2){
        keymap = Some(parameters(2))
      }

      val workingDir = data.get(ContextConstant.FullKey.CONFIG_WORKING_DIRECTORY)

      if(workingDir.isDefined){
        //TODO: if repository exists, just merge the newest commit from master
        val destinationDirectory = new File(workingDir.get + "/" + path)

        if(destinationDirectory.exists && destinationDirectory.isDirectory && !isDirEmpty(destinationDirectory.toPath)){
          //TODO: just update the repository
          println("directory already exists, starting update..")
        } else {
          println("directory does not exist, initialising download..")
          destinationDirectory.mkdirs()
          val command = "git -C " + workingDir.get + " clone " + repoUrl + " " +  path
          resultCode = command.!
        }

        if(resultCode == 0){
          if(keymap.isDefined){
            val result = HashMap[String,String](keymap.get -> (workingDir.get + "/" + path))
            mResult = Some(result)
          }
        } else {
          mResult = None
        }
      }
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        mResult = None
    }


  }

  def isDirEmpty(directory:Path):Boolean = {
    try {
      val dirStream = Files.newDirectoryStream(directory)
      !dirStream.iterator.hasNext
    } catch {
      case e:IOException =>
        true
    }

  }

  override def result: Option[Map[String,String]] = {
    mResult
  }

  override def help(parameters: Array[String]): Unit = ???
}
