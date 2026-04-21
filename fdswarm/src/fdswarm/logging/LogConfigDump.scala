package fdswarm.logging

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext

def dumpLogConfig(): Unit =

  val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val config = ctx.getConfiguration

  println("=== Appenders ===")
  config.getAppenders.forEach { (name, appender) =>

    println(s"$name -> ${appender.getClass.getName}")
  }

  println("\n=== Loggers ===")
  config.getLoggers.forEach { (name, loggerConfig) =>
    println(s"Logger: $name")
    println(s"  Level: ${loggerConfig.getLevel}")
    println(s"  Appenders: ${loggerConfig.getAppenders.keySet}")
  }

  println("\n=== Root Logger ===")
  val root = config.getRootLogger
  println(s"Level: ${root.getLevel}")
  println(s"Appenders: ${root.getAppenders.keySet}")
