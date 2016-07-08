package io.glassdoor.plugin.plugins.preprocessor.database

import io.glassdoor.application.{CommandInterpreter, Parameter, ParameterType}
import io.glassdoor.plugin.Plugin
import slick.driver.SQLiteDriver.api._

/**
  * Created by flosch on 7/8/16.
  */
class DatabaseExtractor extends Plugin{
  var mDatabaseExtractorOptions = new DatabaseExtractorOptions


  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined){
      handleParameters(parameterArray.get)

      if(mDatabaseExtractorOptions.inputDescriptor.isDefined && mDatabaseExtractorOptions.outputDescriptor.isDefined){
        val resolvedInput = data.get(mDatabaseExtractorOptions.inputDescriptor.get)

        if(resolvedInput.isDefined){
          var input = resolvedInput.get

          if(mDatabaseExtractorOptions.subFile.isDefined) input += mDatabaseExtractorOptions.subFile.get

          extractDatabase(input,"")
        }
      }
    }
  }

  def extractDatabase(inputPath:String, outputPath:String):Unit = {
    val db = Database.forURL("jdbc:sqlite:" + inputPath, driver = "org.sqlite.JDBC")
    val action = sql"select * from *".as[(Int)]
    db.run(action).foreach(println)
  }

  def handleParameters(parameterArray:Array[Parameter]):Unit = {
    for(parameter <- parameterArray){
      parameter.paramType match {
        case ParameterType.NamedParameter =>
          parameter.name match {
            case "subfile" | "s" =>
              mDatabaseExtractorOptions.subFile = parameter.value
          }
        case ParameterType.Flag =>
        //TODO
        case ParameterType.Parameter =>
          if(mDatabaseExtractorOptions.inputDescriptor.isEmpty){
            mDatabaseExtractorOptions.inputDescriptor = Some(parameter.name)
          } else if(mDatabaseExtractorOptions.outputDescriptor.isEmpty){
            mDatabaseExtractorOptions.outputDescriptor = Some(parameter.name)
          }
      }
    }
  }

  /**
    * This will be called once you call the ready() method inside your plugin.
    * Please return ALL the changed values here as a map containing the key and the changed value.
    * If you did not change any values, simply return an empty map = Some(Map[String,String]())
    * Returning None here, will be interpreted as an error.
    *
    * @return a map containing all the changed values.
    */
override def result: Option[Map[String, String]] = ???

  override def help(parameters: Array[String]): Unit = ???
}
