package io.glassdoor.application

import io.glassdoor.application.ParameterType.ParameterType

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Object used to parse commands and split parameters into different parameter types.
  */
object CommandInterpreter {

  /**
    * Interprets the given string and returns a command object, when parsed successfully.
    * @param input the command as string
    * @param placeholderParameters there might be placeholders in the command, these should be replaced (not implemented yet, just handover None or nothing).
    * @return if successful Some containing a Command, None otherwise
    */
  def interpret(input:String, placeholderParameters:Option[String] = None):Option[Command] = {
    try {
      Log.debug("input string to interpret: " + input)

      //split into groups, considering double and single quotes
      val commandRegex = "(\"[^\"]*\"|'[^']*'|[\\S]+)+".r
      var inputArray = commandRegex.findAllIn(input).toArray

      //remove leading and ending quotes
      val quoteRegex = "^\\s*[\"']?|[\"']?\\s*$".r
      inputArray = inputArray.map(x => quoteRegex.replaceAllIn(x, ""))

      for(part <- inputArray){
        Log.debug("command part: " + part)
      }

      val inputBuffer = inputArray.toBuffer
      inputBuffer.remove(0)

      val commandName = inputArray(0)
      val commandParameters = inputBuffer.toArray

      if(placeholderParameters.isDefined){
        //TODO: replace placeholders in the input command with the parameters
      }

      Some(Command(commandName, commandParameters))
    } catch {
      case e:Exception =>
        None
    }

  }

  /**
    * Parses the given String array and returns an array of Parameters which
    * contain the correct parameter type and values associated with the parameter.
    * @param input array of already split parameter values
    * @return array of categorized parameters containing the values
    */
  def parseToParameterArray(input:Array[String]):Option[Array[Parameter]]={
    val buffer = ArrayBuffer.empty[Parameter]

    val namedParameter = """^-.+""".r
    val flag = """^\+.+""".r //watchout! this also matches flag full name on purpose
    val flagFullName = """^\+\+.+""".r

    //used to skip the next value after a named parameter
    var pendingNamedParameter = false;

    for(i <- input.indices) {
      input(i) match {
        case namedParameter() =>
          Log.debug("found named parameter: " + input(i))
          //for now - and -- are the same
          val strippedName = input(i).stripPrefix("-").stripPrefix("-") //if prefix is not found, the string is returned unchanged

          Log.debug("input length: " + input.length)
          Log.debug("current index: " + input.length)

          //if param is not empty after stripping the prefix
          if(input.length > (i + 1)){
            Log.debug("input length is larger than index + 1")
            val param = Parameter(strippedName, ParameterType.NamedParameter, Some(input(i+1)))
            buffer.append(param)
            pendingNamedParameter = true
          }
        case flag() =>
          Log.debug("found flag: " + input(i))
          //TODO: remove + or ++ and parse correctly
          if(flagFullName.findFirstIn(input(i)) == None){
            //not fullname, parse each character
            val flags = input(i).stripPrefix("+")

            for(j <- flags.indices){
              val param = Parameter(flags(j).toString, ParameterType.Flag, None)
              buffer.append(param)
            }
          } else {
            //full name, drop the first two chars (++)
            val param = Parameter(input(i).stripPrefix("++"), ParameterType.Flag, None)
            buffer.append(param)
          }
        case _ =>
          Log.debug("found parameter: " + input(i))
          if(!pendingNamedParameter){
            Log.debug("no pending named parameter")
            val param = Parameter(input(i), ParameterType.Parameter, None)
            buffer.append(param)
          } else {
            Log.debug("skipped, because of pending named parameter")
          }
          pendingNamedParameter = false
      }
    }

    Some(buffer.toArray)
  }
}

//TODO: parameters could be optional..
case class Command(name:String, parameters:Array[String])
case class Parameter(name:String, paramType:ParameterType, value:Option[String])

object ParameterType extends Enumeration {
  type ParameterType = Value
  val  Parameter, NamedParameter, Flag = Value
}
