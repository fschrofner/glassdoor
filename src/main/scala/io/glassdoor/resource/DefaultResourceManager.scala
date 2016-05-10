package io.glassdoor.plugin.resource

import java.io.File

import scala.collection.immutable.HashMap
import io.glassdoor.resource.Resource
import io.glassdoor.application._

/**
  * Created by Florian Schrofner on 4/15/16.
  */
class DefaultResourceManager extends ResourceManager{
  var mResources:Map[String,Resource] =  new HashMap[String,Resource]

  //TODO: manage list of resource instances with type
  //TODO: somehow allow installation of new resources

  override def installResource(name: String, context:Context):Unit = {
    println("install resource called!")
    //TODO: check if list of resources is present, otherwise can not execute. update needs to be doen manually
    //TODO: install resource using git and save in resource map

    val resourceDirOpt = context.getResolvedValue(ContextConstant.FullKey.CONFIG_RESOURCE_DIRECTORY)

    if(resourceDirOpt.isDefined){
      val resourceDir = resourceDirOpt.get
      val resourceRepositoryPath = resourceDir + "/" + ResourceManagerConstant.Path.RESOURCE_REPOSITORY_DIRECTORY
      //TODO: find resource repositories in repository directory

      val resourceRepositoryDir = new File(resourceRepositoryPath)

      //get all subdirs, look up resource name in the files found in the subdirs
      val subdirPaths = resourceRepositoryDir.list()

      if(subdirPaths != null){
        println("number of subdirs: " + subdirPaths.length)
      } else {
        //directory might not exist!
        println("number of subdirs: 0")
      }
    } else {
      println("resource directory is not defined!")
    }
  }

  override def getResource(name:String):Option[Resource] = {
    mResources.get(name)
  }

  override def loadResources(context:Context):Unit = {
    //TODO: load resources from resource directory
  }

}
