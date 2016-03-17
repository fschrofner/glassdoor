package io.glassdoor.interface

import com.googlecode.lanterna.input.{KeyType, KeyStroke}
import com.googlecode.lanterna.terminal.ansi.UnixTerminal
import com.googlecode.lanterna.terminal.{DefaultTerminalFactory, Terminal}

/**
  * Created by Florian Schrofner on 3/15/16.
  */
class CommandLineInterface extends UserInterface{

   def test(): Unit = {
     //val terminal = new DefaultTerminalFactory().createTerminal()
     val terminal = new UnixTerminal()
     terminal.enterPrivateMode()
     terminal.setCursorVisible(true)
     terminal.disableSpecialCharacters()
     //terminal.setTitle("TEST")
     //terminal.setCBreak(true)
     //terminal.setEcho(false)

     //var currentText = new StringBuilder()
     var loop = true

     var keyStroke:KeyStroke = null

     while(keyStroke == null || keyStroke.getKeyType != KeyType.Escape){
       //ALWAYS READS EOF INSTANTLY!!
       keyStroke = terminal.readInput() //blocking

       keyStroke.getKeyType match {
         case KeyType.Enter =>
           //TODO: handle enter/interpret command
           terminal.putCharacter('e')
           //val graphics = terminal.newTextGraphics()
           //graphics.putString(0,0,"TESTSTRING")

           loop = false
         case KeyType.Tab =>
           //TODO: handle tab/autocomplete
           terminal.putCharacter('t')
         case KeyType.Character =>
           //TODO: add character to
           terminal.putCharacter('c')
           //currentText += keyStroke.getCharacter
         case _ =>
           //default
           Thread.sleep(10)
       }

     }

     terminal.exitPrivateMode()
   }
}

