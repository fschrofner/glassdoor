package io.glassdoor.plugin.plugins.analyser.regex

/**
  * Created by flosch on 7/4/16.
  */
class RegexOptions {
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