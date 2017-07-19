package io.glassdoor.plugin.plugins.installer.git

import java.io.{File, IOException}
import java.nio.file.{Files, Path}

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.sys.process._

/**
  * A plugin used to download and update data from Git repositories.
  * Could be used to update/receive resources or other plugins.
  * Created by Florian Schrofner on 4/15/16.
  */
class GitInstaller extends Plugin {

  var mResult:Option[Map[String,String]] = None


  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    if(parameters.length == 3){
      new DynamicValues(uniqueId, Some(Array[String]()), Some(Array(parameters(0))))
    } else {
      new DynamicValues(uniqueId, Some(Array[String]()), Some(Array[String]()))
    }
  }

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
        setErrorMessage("error: incorrect number of parameters!")
        ready()
        return
      }

      val executor = new SystemCommandExecutor

      if(repoUrl.isDefined && path.isDefined){
        showEndlessProgress()

        val destinationDirectory = new File(path.get)

        if(destinationDirectory.exists && destinationDirectory.isDirectory && !isDirEmpty(destinationDirectory.toPath)){
          Log.debug("directory already exists, starting update..")

          val command = Seq("git","-C",path.get,"pull")

          executor.executeSystemCommand(command)
        } else {
          Log.debug("directory does not exist, initialising download..")
          destinationDirectory.mkdirs()

          val command = Seq("git","clone",repoUrl.get, path.get)

          executor.executeSystemCommand(command)
        }

        if(executor.lastCommandSuccessful){
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

}
