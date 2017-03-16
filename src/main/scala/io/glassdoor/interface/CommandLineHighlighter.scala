package io.glassdoor.interface

import java.awt.Color

import io.glassdoor.application.{CommandInterpreter, ParameterType}
import org.jline.reader.{Highlighter, LineReader}
import org.jline.utils.{AttributedString, AttributedStringBuilder, AttributedStyle}

/**
  * Created by Florian Schrofner on 3/15/17.
  */
class CommandLineHighlighter(pluginCommandList:Option[Array[String]], contextKeys:Option[Array[String]]) extends Highlighter {

  override def highlight(reader: LineReader, buffer: String): AttributedString = {
    val builder = new AttributedStringBuilder()

    val command = CommandInterpreter.interpret(buffer)

    if(command.isDefined){
      var markValid = false
      var commandLength = command.get.name.length

      if(pluginCommandList.isDefined && pluginCommandList.get.contains(command.get.name)){
        markValid = true
      }

      val parameterStrings = command.get.parameters


      if(parameterStrings.length > 0){
        val params = CommandInterpreter.parseToParameterArray(parameterStrings)

        if(params.isDefined && params.get.length > 0){

          //if the first parameter is an unnamed one, it might be a command
          //highlight everything, if it is found in the command list
          if(params.get(0).paramType == ParameterType.Parameter){
            val combinedString = command.get.name + " " + params.get(0).name
            if(pluginCommandList.get.contains(combinedString)){
              //the whole command should be marked as valid
              commandLength = combinedString.length
            }
          }
        }
      }

      //TODO: highlight context keys, when they are not the first
      var singleQuoteActive = false
      var doubleQuoteActive = false

      for (i <- 0 until buffer.length) {
        //TODO: adapt styling
        if(i < commandLength){
          builder.style(builder.style().bold())
          if(markValid){
            builder.style(builder.style().foreground(AttributedStyle.BLUE))
          } else {
            builder.style(builder.style().foreground(AttributedStyle.RED))
          }
        } else {
          builder.style(builder.style().boldOff())

          if(!singleQuoteActive && !doubleQuoteActive){
            builder.style(builder.style().foreground(AttributedStyle.WHITE))
          } else {
            builder.style(builder.style().foreground(AttributedStyle.YELLOW))
          }

          if(buffer(i) == '"' && !singleQuoteActive){
            doubleQuoteActive = !doubleQuoteActive

            //needed so that the quotes will be highlighted
            builder.style(builder.style().foreground(AttributedStyle.YELLOW))
          } else if(buffer(i) == '\'' && !doubleQuoteActive){
            singleQuoteActive = !singleQuoteActive

            //needed so that the quotes will be highlighted
            builder.style(builder.style().foreground(AttributedStyle.YELLOW))
          }
        }

        builder.append(buffer(i))
      }
    }

    builder.toAttributedString
  }
}
