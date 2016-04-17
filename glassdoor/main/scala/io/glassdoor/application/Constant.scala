package io.glassdoor.application

/**
  * Created by Florian Schrofner on 3/17/16.
  */
//TODO: the constants should be moved to the matching classes
object Constant {
  //predefined regexes
  object Regex {
    val DESCRIPTOR_SPLIT_REGEX = """\."""
    val REGEX_PATTERN_DEX = """^.*\.[Dd][Ee][Xx]$"""
    val REGEX_PATTERN_EMAIL = """.+@.+\..{2,}"""
  }
}
