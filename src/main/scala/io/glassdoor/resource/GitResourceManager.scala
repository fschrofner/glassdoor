package io.glassdoor.plugin.resource

import java.io.File

import com.typesafe.config.ConfigFactory
import io.glassdoor.bus.{MessageEvent, EventBus, Message}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginParameters
import io.glassdoor.plugin.manager.PluginManagerConstant

import scala.collection.immutable.HashMap
import io.glassdoor.resource.Resource
import io.glassdoor.application._
import scala.collection.JavaConverters._

/**
  * Created by Florian Schrofner on 4/15/16.
  */
class GitResourceManager extends ResourceManager{
  var mResources:Map[String,Resource] =  new HashMap[String,Resource]
  var mAvailableResources:Map[String,Resource] = new HashMap[String, Resource]

  //TODO: manage list of resource instances with type
  //TODO: somehow allow installation of new resources

  override def installResource(name: String, context:Context):Unit = {
    if(!mResources.contains(name)){
      if(mAvailableResources.contains(name)){
        Log.debug("resource found! installing..")
        //call git plugin and download the resource from the repository
        val resource = mAvailableResources.get(name)
        val destinationPath = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

        if(resource.isDefined && destinationPath.isDefined && resource.get.repository.isDefined){
          val parameterArray = Array(resource.get.repository.get, destinationPath.get + "/" + resource.get.name)
          val parameters = new Command(GitResourceManagerConstant.Repository.PluginCommand, parameterArray)
          val message = new Message(ControllerConstant.Action.ApplyPlugin, Some(parameters))
          EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))

          //TODO: retrieve result and save into mResources!
        }
      } else {
        Log.debug("error: resource cannot be found")
      }
    } else {
      Log.debug("error: resource already installed")
    }
  }

  override def getResource(name:String):Option[Resource] = {
    mResources.get(name)
  }

  def buildAvailableResourceIndex(context:Context):Unit = {
    Log.debug("build available resource index called!")
    //TODO: check if list of resources is present, otherwise can not execute. update needs to be done manually
    //TODO: install resource using git and save in resource map

    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

    if(resourceDirOpt.isDefined){
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + GitResourceManagerConstant.Path.ResourceRepositoryDirectory

      //TODO: find resource repositories in repository directory
      val resourceRepositoryDir = new File(resourceRepositoryPath)

      //note: there can be multiple resource repositories!
      //get all subdirs, look up resource name in the files found in the subdirs
      val subdirPaths = resourceRepositoryDir.list()

      if(subdirPaths != null){
        Log.debug("number of subdirs: " + subdirPaths.length)
        if(subdirPaths.length > 0){
          Log.debug("traversing through the repositories..")

          val availableResources:scala.collection.mutable.HashMap[String,Resource] = new scala.collection.mutable.HashMap[String,Resource]

          for(subdir <- subdirPaths){
            Log.debug("subdir: " + subdir)
            val repositoryDir = new File(resourceRepositoryPath + "/" + subdir)
            val files = repositoryDir.listFiles()

            if(files != null && files.length > 0){
              for(file <- files){

                //only handle .conf files
                val fileName = file.getName
                val extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())

                if(extension == GitResourceManagerConstant.Repository.FileExtension){
                  val repositoryConfig = ConfigFactory.parseFile(file)
                  val resourceList = repositoryConfig.getConfigList(GitResourceManagerConstant.Key.Resources).asScala

                  for(resourceConfig <- resourceList){
                    val name = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceName)
                    val typ = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceType)
                    val repository = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceRepository)
                    val resource = new Resource(name, typ, Some(repository))
                    availableResources.put(name, resource)
                    Log.debug("added available resource: " + name + "[" + typ + "]")
                  }
                }
              }
            } else {
              Log.debug("subdir is empty!")
            }
          }

          mAvailableResources = availableResources.toMap
        }
      } else {
        //directory might not exist!
        Log.debug("number of subdirs: 0")
      }
    } else {
      Log.debug("resource directory is not defined!")
    }
  }
  def buildResourceIndex(context:Context):Unit = {

    //TODO: load resources from resource directory
    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

    if(resourceDirOpt.isDefined){
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + GitResourceManagerConstant.Path.ResourceRepositoryDirectory
      //TODO: find resource repositories in repository directory

      val resourceRepositoryDir = new File(resourceRepositoryPath)

      //get all subdirs, look up resource name in the files found in the subdirs
      val subdirPaths = resourceRepositoryDir.list()

      if(subdirPaths != null){
        Log.debug("number of subdirs: " + subdirPaths.length)
        if(subdirPaths.length > 0){
          //TODO: check if repositories subdir exists
        }
      } else {
        //directory might not exist!
        Log.debug("number of subdirs: 0")
      }
    } else {
      Log.debug("resource directory is not defined!")
    }
  }

  override def initialise(context: Context): Unit = {
    buildAvailableResourceIndex(context)
    //buildResourceIndex(context)
  }

  override def updateAvailableResourceIndex(context: Context): Unit = {
    val repositoriesOpt = context.getResolvedArray(ContextConstant.FullKey.ConfigResourceRepository)
    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

    if(resourceDirOpt.isDefined && repositoriesOpt.isDefined) {
      val repositories = repositoriesOpt.get
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + GitResourceManagerConstant.Path.ResourceRepositoryDirectory

      for(repository <- repositories){
        //TODO: download the repositories to the resource repository path, if they do not exist, pull otherwise
        //TODO: make sure that the command line does not think that it's ready again, although there is still a download ongoing
        //EventBus.publish(MessageEvent(ControllerConstant.Channel, Message(ControllerConstant.Action.ApplyPlugin, Plugi))
      }
    }
  }
}

object GitResourceManagerConstant {
  object Repository {
    val FileExtension = "conf"
    val PluginCommand = "git"
  }
  object Key {
    val Resources = "resources"
    val ResourceName = "name"
    val ResourceType = "type"
    val ResourceRepository = "repository"
  }
  object Path {
    val ResourceRepositoryDirectory = "repositories"
  }
}