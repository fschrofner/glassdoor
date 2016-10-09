appender("CONSOLE", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "[%-5level] %msg%n"
  }
}

root(OFF, ["CONSOLE"])

logger("jadx", OFF)
logger("j", OFF)
