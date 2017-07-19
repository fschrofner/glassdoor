package io.glassdoor.plugin.resource

import java.io.File

import com.typesafe.config.ConfigFactory
import io.glassdoor.bus.{EventBus, Message, MessageEvent}
import io.glassdoor.controller.ControllerConstant
import io.glassdoor.plugin.PluginParameters
import io.glassdoor.plugin.manager.PluginManagerConstant

import scala.collection.immutable.HashMap
import io.glassdoor.resource.Resource
import io.glassdoor.application._
import io.glassdoor.plugin.resource.ResourceManagerConstant.{ResourceErrorCode, ResourceSuccessCode}
import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._

/**
  * Created by Florian Schrofner on 4/15/16.
  */
class GitResourceManager extends ResourceManager{
  var mResources:Map[String,Resource] =  new HashMap[String,Resource]
  var mAvailableResources:Map[String,Resource] = new HashMap[String, Resource]

  override def installResource(name: String, context:Context):Unit = {
    if(!mResources.contains(name)){
      if(mAvailableResources.contains(name)){
        Log.debug("resource found! installing..")
        //call git plugin and download the resource from the repository
        val resource = mAvailableResources.get(name)
        val destinationPath = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

        if(resource.isDefined && destinationPath.isDefined && resource.get.repository.isDefined){
          val keymap = ContextConstant.FullKey.ResourceDictionary + ContextConstant.DescriptorSplit + resource.get.name
          val parameterArray = Array(keymap, resource.get.repository.get, destinationPath.get + "/" + GitResourceManagerConstant.Path.ResourceDirectory + "/" + resource.get.name)
          val parameters = new Command(GitResourceManagerConstant.Repository.PluginCommand, parameterArray)
          val message = new Message(ControllerConstant.Action.ApplyPlugin, Some(parameters))
          EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
          //TODO: retrieve result and save into mResources!
        }
      } else {
        Log.debug("error: resource cannot be found")
        sendErrorMessage(mAvailableResources.get(name), ResourceErrorCode.ResourceNotFound, None)
      }
    } else {
      Log.debug("error: resource already installed")
      sendErrorMessage(mAvailableResources.get(name), ResourceErrorCode.ResourceAlreadyInstalled, None)
    }
  }


  override def removeResource(name: String, context: Context): Unit = {
    if(mResources.contains(name)){
      val resource = mResources.get(name).get

      if(resource.directory.isDefined){
        val file = new File(resource.directory.get)
        FileUtils.deleteDirectory(file)
        mResources = mResources - name

        val fullName = ContextConstant.Keymap.Resource + ContextConstant.DescriptorSplit + resource.kind + ContextConstant.DescriptorSplit + resource.name
        removeResourcesFromContext(Array(fullName))

        sendSuccessMessage(Some(resource), ResourceSuccessCode.ResourceSuccessfullyRemoved)
      }
    } else {
      Log.debug("error: resource not found!")
      sendErrorMessage(None, ResourceErrorCode.ResourceNotFound, None)
    }
  }

  override def handleResourceInstallCallback(keymap: Map[String, String]): Unit = {
    val resources:scala.collection.mutable.HashMap[String,Resource] = new scala.collection.mutable.HashMap[String,Resource]

    for(resourceDescriptor <- keymap){
      val resourceName = resourceDescriptor._1.substring(resourceDescriptor._1.lastIndexOf(ContextConstant.DescriptorSplit) + 1, resourceDescriptor._1.length)

      val availableResourceOpt = mAvailableResources.get(resourceName)

      if(availableResourceOpt.isDefined){
        val availableResource = availableResourceOpt.get
        val directory = resourceDescriptor._2
        val resource = Resource(availableResource.name, availableResource.kind, Some(directory), availableResource.repository)
        Log.debug("successfully installed resource: " + resourceName)
        Log.debug("adding path to resource: " + directory)
        resources.put(resource.name, resource)
      }
    }

    mResources = mResources ++ resources

    //send success messages
    for(resource <- resources){
      sendSuccessMessage(Some(resource._2), ResourceSuccessCode.ResourceSuccessfullyInstalled)
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

            if(files != null && files.nonEmpty){
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
                    val resource = new Resource(name, typ, None, Some(repository))
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
    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

    if(resourceDirOpt.isDefined){
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + GitResourceManagerConstant.Path.ResourceDirectory
      val resourceRepositoryDir = new File(resourceRepositoryPath)

      //get all subdirs, look up resource name in the files found in the subdirs
      val subdirPaths = resourceRepositoryDir.list()

      if(subdirPaths != null){
        Log.debug("number of subdirs: " + subdirPaths.length)

        //checking subdirs
        if(subdirPaths.length > 0){
          val resources:scala.collection.mutable.HashMap[String,Resource] = new scala.collection.mutable.HashMap[String,Resource]

          for(name <- subdirPaths){
            //first look into folder, if there is a .conf file
            val directoryPath = resourceRepositoryDir.getAbsolutePath + "/" + name

            var resource = readResourceFromLocalConfFile(directoryPath)

            if(resource.isEmpty){
              Log.debug("no local conf file provided for: " + name + ", getting information from repository")
              resource = readResourceFromResourceRepository(name, directoryPath)
            }

            if(resource.isDefined){
              resources.put(resource.get.name, resource.get)
            }
          }

          mResources = resources.toMap
        }
      } else {
        //directory might not exist!
        Log.debug("number of subdirs: 0")
      }
    } else {
      Log.debug("resource directory is not defined!")
    }
  }

  def readResourceFromLocalConfFile(path:String):Option[Resource] = {
    val directory = new File(path)
    val files = directory.listFiles()

    if(files != null && files.nonEmpty){
      for(file <- files){
        //only handle .conf files
        val fileName = file.getName
        val extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())

        if(extension == GitResourceManagerConstant.Repository.FileExtension) {
          val repositoryConfig = ConfigFactory.parseFile(file)
          val resourceConfig = repositoryConfig.getConfig(GitResourceManagerConstant.Repository.ConfigResourceRootElement)

          val name = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceName)
          val typ = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceType)
          val repository = resourceConfig.getString(GitResourceManagerConstant.Key.ResourceRepository)
          val resource = Resource(name, typ, Some(path), Some(repository))
          Log.debug("found local conf for: " + name + "[" + typ + "]")
          return Some(resource)
        }
      }
    }
    None
  }

  def readResourceFromResourceRepository(name:String, path:String):Option[Resource] = {
    val availableResourceOpt = mAvailableResources.get(name)
    if(availableResourceOpt.isDefined){
      val availableResource = availableResourceOpt.get
      val directory = path
      val resource = Resource(availableResource.name, availableResource.kind, Some(directory), availableResource.repository)
      Log.debug("found resource in repository: " + name)
      Log.debug("adding path to resource: " + directory)
      Some(resource)
    } else {
      None
    }
  }

  override def initialise(context: Context): Unit = {
    buildAvailableResourceIndex(context)
    buildResourceIndex(context)

    if(mResources.size > 0){
      //add resources to context
      val changedResources:scala.collection.mutable.Map[String, String] = new scala.collection.mutable.HashMap[String, String]

      for(resource <- mResources){
        if(resource._2.directory.isDefined){
          val descriptor = ContextConstant.Keymap.Resource + ContextConstant.DescriptorSplit + resource._2.kind + ContextConstant.DescriptorSplit + resource._2.name
          val directory = resource._2.directory.get
          changedResources.put(descriptor, directory)
        }
      }

      addResourcesToContext(changedResources.toMap)
    }
  }

  override def updateAvailableResourceIndex(context: Context): Unit = {
    val repositoriesOpt = context.getResolvedArray(ContextConstant.FullKey.ConfigResourceRepository)
    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.ConfigResourceDirectory)

    if(resourceDirOpt.isDefined && repositoriesOpt.isDefined) {
      val repositories = repositoriesOpt.get
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + GitResourceManagerConstant.Path.ResourceRepositoryDirectory

      for(repository <- repositories){
        val parameterArray = Array(repository, resourceRepositoryPath + "/" + getRepositorySimpleName(repository))
        val parameters = new Command(GitResourceManagerConstant.Repository.PluginCommand, parameterArray)
        val message = new Message(ControllerConstant.Action.ApplyPlugin, Some(parameters))
        EventBus.publish(new MessageEvent(ControllerConstant.Channel, message))
        //TODO: make sure that the command line does not think that it's ready again, although there is still a download ongoing
        //TODO: buildAvailableResourceIndex after update has finished!
      }
    }
  }

  def getRepositorySimpleName(repository:String):String = {
    val lastPartRegex = "[^/]+$".r
    val gitRegex = "\\.git$".r
    var simpleName = ""

    //retrieve last part of url (after last slash)
    val lastPart = lastPartRegex.findFirstIn(repository)

    if(lastPart.isDefined){
      //remove .git from the last part
      simpleName = gitRegex.replaceAllIn(lastPart.get, "")
    }

    simpleName
  }
}


object GitResourceManagerConstant {
  object Repository {
    val FileExtension = "conf"
    val ConfigResourceRootElement = "resource"
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
    val ResourceDirectory = "resources"
  }
}