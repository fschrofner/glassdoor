package io.glassdoor.plugin.plugins.preprocessor.database

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import io.glassdoor.application._
import io.glassdoor.plugin.{DynamicValues, Plugin}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 7/8/16.
  */
class DatabaseExtractor extends Plugin{
  var mDatabaseExtractorOptions = new DatabaseExtractorOptions
  val mSystemCommandExecutor = new SystemCommandExecutor

  var mResult:Option[Map[String, String]] = None

  /**
    * This method should only be overridden, when specifying either dynamic dependencies or dynamic changes in the manifest.
    * This method will then be called with the given parameters, before the plugin can be scheduled.
    * The result should contain the values requested. Specify None, if you did not specify this value as dynamic.
    * None values will be ignored, to change your dynamic dependency to an empty dependency wrap an empty string array in Some = Some(Array[String]()).
    */
  override def resolveDynamicValues(parameters: Array[String]): DynamicValues = {
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined && parameterArray.get.length >= 2){
      handleParameters(parameterArray.get)

      var inputDescriptor:Option[Array[String]] = None
      var outputDescriptor:Option[Array[String]] = None

      if(mDatabaseExtractorOptions.inputDescriptor.isDefined){
        inputDescriptor = Some(Array(mDatabaseExtractorOptions.inputDescriptor.get))
      }

      if(mDatabaseExtractorOptions.outputDescriptor.isDefined){
        outputDescriptor = Some(Array(mDatabaseExtractorOptions.outputDescriptor.get))
      }

      return DynamicValues(uniqueId, inputDescriptor, outputDescriptor)
    }

    DynamicValues(uniqueId, Some(Array[String]()), Some(Array[String]()))
  }

  /**
    * This is the method called, when your plugin gets launched.
    *
    * @param data       a map containing all the values you defined as dependencies and changes, as well as all config values of glassdoor
    * @param parameters the parameters that were provided, when your plugin was called
    */
  override def apply(data: Map[String, String], parameters: Array[String]): Unit = {
    Log.debug("apply database extractor called")
    val parameterArray = CommandInterpreter.parseToParameterArray(parameters)

    if(parameterArray.isDefined && parameterArray.get.length >= 2){
      handleParameters(parameterArray.get)

      if(mDatabaseExtractorOptions.inputDescriptor.isDefined && mDatabaseExtractorOptions.outputDescriptor.isDefined){
        val resolvedInput = data.get(mDatabaseExtractorOptions.inputDescriptor.get)

        if(resolvedInput.isDefined){
          var input = resolvedInput.get

          if(mDatabaseExtractorOptions.subFile.isDefined) input += File.separator + mDatabaseExtractorOptions.subFile.get

          val workingDirectory = data.get(ContextConstant.FullKey.ConfigWorkingDirectory)

          if(workingDirectory.isDefined){

            val outputFilePath = workingDirectory.get + File.separator + ContextConstant.Key.ExtractedDatabase + File.separator + splitDescriptor(mDatabaseExtractorOptions.outputDescriptor.get)(1) + "/result.log"
            val success = extractDatabase(input,outputFilePath)

            if(success){
              val result = HashMap[String,String](mDatabaseExtractorOptions.outputDescriptor.get -> outputFilePath)
              mResult = Some(result)
            }
          }
        }
      }
    } else {
      mResult = None
      if(parameterArray.isDefined){
        setErrorMessage("error: not enough parameters")
      } else {
        setErrorMessage("error: could not parse parameters")
      }
    }

    ready()
  }

  def splitDescriptor(descriptor:String):Array[String] = {
    descriptor.split(Constant.Regex.DescriptorSplitRegex)
  }

  def extractDatabase(inputPath:String, outputPath:String):Boolean = {
    Log.debug("extract database called")
    val arrayBuffer = ArrayBuffer[String]()
    val tableNames = getTableNames(inputPath)

    if(tableNames.isDefined){
      for(tableName <- tableNames.get){
        arrayBuffer ++= getTableContent(inputPath, tableName)
      }

      val file = new File(outputPath)
      file.getParentFile.mkdirs()

      val bw = new BufferedWriter(new FileWriter(file, true))

      val newLine = System.getProperty("line.separator")

      for(line <- arrayBuffer){
        bw.write(line + newLine)
      }

      bw.close()
      return true
    } else {
      return false
    }
  }

  def getTableNames(inputPath:String): Option[Array[String]] =  {
    val tables = ArrayBuffer[String]()
    val command = new ArrayBuffer[String]()
    command.append("sqlite3")
    command.append(inputPath)
    command.append(".tables")
    val resultString = mSystemCommandExecutor.executeSystemCommand(command)
    if(resultString.isDefined){
      Log.debug("extracted tables: " + resultString.get)
      for(line <- scala.io.Source.fromString(resultString.get).getLines()){
        tables.append(line)
      }
    } else {
      setErrorMessage("error: could not extract table names!")
      return None
    }
    Some(tables.toArray)
  }

  def getTableContent(inputPath:String, tableName:String): Array[String] = {
    val content = ArrayBuffer[String]()
    val command = ArrayBuffer[String]()
    command.append("sqlite3")
    command.append("-header")
    command.append("-csv")
    command.append(inputPath)
    command.append("select * from " + tableName + ";")
    val resultString = mSystemCommandExecutor.executeSystemCommand(command)
    if(resultString.isDefined){
      Log.debug("extracted data from table " + tableName + ": " + resultString.get)
      for(line <- scala.io.Source.fromString(resultString.get).getLines()){
        content.append(line)
      }
    }
    content.toArray
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
            Log.debug("parsed input descriptor")
          } else if(mDatabaseExtractorOptions.outputDescriptor.isEmpty){
            mDatabaseExtractorOptions.outputDescriptor = Some(parameter.name)
            Log.debug("parsed output descriptor")
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
  override def result: Option[Map[String, String]] = {
    mResult
  }

}
