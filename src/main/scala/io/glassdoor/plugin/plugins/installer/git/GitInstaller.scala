package io.glassdoor.plugin.plugins.installer.git

import java.io.{IOException, File}
import java.nio.file.{Files, Path}

import io.glassdoor.application.{Log, ContextConstant, Constant, Context}
import io.glassdoor.plugin.Plugin
import scala.collection.immutable.HashMap
import scala.sys.process._

/**
  * A plugin used to download and update data from Git repositories.
  * Could be used to update/receive resources or other plugins.
  * Created by Florian Schrofner on 4/15/16.
  */
class GitInstaller extends Plugin {

  var mResult:Option[Map[String,String]] = None

  override def apply(data:Map[String,String], parameters: Array[String]): Unit = {
    try {
      var repoUrl:Option[String] = None
      var path:Option[String] = None
      var keymapDescription:Option[String] = None

      if(parameters.length == 2) {
        repoUrl = Some(parameters(0))
        path = Some(parameters(1))
      } else if(parameters.length == 3) {
        keymapDescription = Some(parameters(0))
        repoUrl = Some(parameters(1))
        path = Some(parameters(2))
      } else {
        //TODO: send error message
        Log.debug("incorrect number of parameters!")
      }

      var resultCode = 1

      //TODO: if the gitupdater is used to install a plugin, it might be needed to save it somewhere
      //TODO: it is needed to return the result to the correct manager (resource or plugin)

      val workingDir = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

      if(workingDir.isDefined && repoUrl.isDefined && path.isDefined){
        showEndlessProgress()

        //TODO: if repository exists, just merge the newest commit from master
        val destinationDirectory = new File(workingDir.get + "/" + path.get)

        if(destinationDirectory.exists && destinationDirectory.isDirectory && !isDirEmpty(destinationDirectory.toPath)){
          //TODO: just update the repository
          Log.debug("directory already exists, starting update..")
        } else {
          Log.debug("directory does not exist, initialising download..")
          destinationDirectory.mkdirs()

          val stdout = new StringBuilder
          val stderr = new StringBuilder

          val command = "git -C " + workingDir.get + " clone " + repoUrl.get + " " +  path.get
          resultCode = command ! ProcessLogger(stdout append _, stderr append _)
        }

        if(resultCode == 0){
          if(keymapDescription.isDefined){
            val result = HashMap[String,String](keymapDescription.get -> path.get)
            mResult = Some(result)
          }
        } else {
          mResult = None
        }
      }
    } catch {
      case e:ArrayIndexOutOfBoundsException =>
        mResult = None
    } finally {
      ready()
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
