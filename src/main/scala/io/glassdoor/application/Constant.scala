package io.glassdoor.application

/**
  * Created by Florian Schrofner on 3/17/16.
  */
//TODO: the constants should be moved to the matching classes
object Constant {
  //predefined regexes
  object Regex {
    val DescriptorSplitRegex = """\."""
    val RegexPatternDex = """^.*\.[Dd][Ee][Xx]$"""
    val RegexPatternEmail = """.+@.+\..{2,}"""
  }

  object Parameter {
    val Any = "*"
  }
}
