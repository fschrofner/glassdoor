package io.glassdoor.plugin.plugins.analyser.regex

import io.glassdoor.plugin.plugins.analyser.regex.RegexSearchBackend.RegexSearchBackend

/**
  * Created by flosch on 7/4/16.
  */
class RegexOptions {
  var searchBackend:RegexSearchBackend = RegexSearchBackend.Grep
  var singleRegex:Option[String] = None
  var overwrite = false
  var patternMatcher = PatternMatcher.Extended
  var showLineNumber = false
  var onlyMatching = false
  var noFileName = false
  var ignoreCase = false
  var printHeaders = false
}

object PatternMatcher extends Enumeration {
  type PatternMatcher= Value
  val  Extended, Strings, Basic, Perl = Value
}

object RegexSearchBackend extends Enumeration {
  type RegexSearchBackend = Value
  val  Grep, TheSilverSearcher = Value
}