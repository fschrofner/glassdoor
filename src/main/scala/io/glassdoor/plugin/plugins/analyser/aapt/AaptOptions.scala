package io.glassdoor.plugin.plugins.analyser.aapt

/**
  * Created by Florian Schrofner on 2/5/17.
  */
class AaptOptions {
  var command = AaptCommand.Dump
  var dumpType = DumpType.Badging
}

object AaptCommand extends Enumeration {
  type AaptCommand = Value
  val  Dump = Value
}

object DumpType extends Enumeration {
  type DumpType = Value
  val Strings, Badging, Permissions, Resources, Configurations, XmlTree, XmlStrings = Value
}