appender("CONSOLE", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "[%-5level] %msg%n"
  }
}

appender("FILE", FileAppender) {
    file = "./logs/glassdoor.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%-5level] %msg%n"
    }
}

root(OFF, ["CONSOLE"])

logger("jadx", OFF)
logger("j", OFF)
