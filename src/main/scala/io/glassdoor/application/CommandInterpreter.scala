package io.glassdoor.application

object CommandInterpreter {
  def interpret(input:String, placeholderParameters:Option[String] = None):Option[Command] = {
    try {
      //TODO: also handle spaces in quotes!! (regex maybe?)
      val inputArray = input.split(" ")
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
}

//TODO: parameters could be optional..
case class Command(name:String, parameters:Array[String])
