package io.glassdoor.plugin.resource

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

  override def installResource(name: String):Unit = {
    //TODO: install resource using git and save in resource map
  }

  override def getResource(name:String):Option[Resource] = {
    mResources.get(name)
  }

  override def loadResources(context:Context):Unit = {
    //TODO: load resources from resource directory
  }

}
