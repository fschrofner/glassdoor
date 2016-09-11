appender("CONSOLE", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "[%-5level] %msg%n"
  }
}

root(DEBUG, ["CONSOLE"])

logger("jadx", OFF)
logger("j", OFF)
