package io.glassdoor.application

import com.typesafe.scalalogging.{StrictLogging, Logger}


/**
  * Created by Florian Schrofner on 6/14/16.
  */
object Log extends StrictLogging{

  def debug(message:String):Unit = {
    logger.debug(message)
  }
}
