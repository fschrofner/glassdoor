package io.glassdoor.interface

import java.awt.Color

import io.glassdoor.application.{CommandInterpreter, Log, ParameterType}
import org.jline.reader.{Highlighter, LineReader}
import org.jline.utils.{AttributedString, AttributedStringBuilder, AttributedStyle}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Florian Schrofner on 3/15/17.
  */
class CommandLineHighlighter(pluginCommandList:Option[Array[String]], contextKeys:Option[Array[String]]) extends Highlighter {

  override def highlight(reader: LineReader, buffer: String): AttributedString = {
    val builder = new AttributedStringBuilder()

    Log.debug("interpreting command for syntax highlighting..")

    val command = CommandInterpreter.interpret(buffer)

    if(command.isDefined){
      var markValid = false
      val validContexts = ArrayBuffer[CommandPart]()
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

        for(param <- params.get){
          if(param.paramType == ParameterType.Parameter){
            if(contextKeys.isDefined && contextKeys.get.contains(param.name)){
              val parts = findCommandParts(param.name, buffer)
              validContexts.appendAll(parts)
            }
          } else if(param.paramType == ParameterType.NamedParameter){
            if(contextKeys.isDefined && param.value.isDefined && contextKeys.get.contains(param.value.get)){
              val parts = findCommandParts(param.value.get, buffer)
              validContexts.appendAll(parts)
            }
          }
        }
      }

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

          if(!singleQuoteActive && !doubleQuoteActive && validContexts.exists(x => x.start <= i && x.end >= i)){
            builder.style(builder.style().bold())
            builder.style(builder.style().foreground(AttributedStyle.GREEN))
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

  def findCommandParts(textToFind: String, buffer:String) : Array[CommandPart] = {
    var searchIndex = 0
    val parts = ArrayBuffer[CommandPart]()

    while(buffer.indexOf(textToFind, searchIndex) != -1){
      val startIndex = buffer.indexOf(textToFind, searchIndex)
      val endIndex = startIndex + textToFind.length
      parts.append(CommandPart(startIndex, endIndex))
      searchIndex = endIndex + 1
    }

    parts.toArray
  }
}

case class CommandPart(start:Int, end:Int)
