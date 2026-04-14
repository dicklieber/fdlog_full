package fdswarm.util

import fdswarm.io.DirectoryProvider
import fdswarm.logging.LogEventFieldNames
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory

object LoggingConfigurator:

  def addFileAppender(directoryProvider: DirectoryProvider): Unit =
    val logDir = directoryProvider()
    os.makeDir.all(logDir)

    val logFile = logDir / "fdswarm.log"
    val logPattern = logDir / "fdswarm-%d{yyyy-MM-dd}.log.gz"
    val accessLogFile = logDir / "access.log"

    val builder = ConfigurationBuilderFactory.newConfigurationBuilder()
    builder.setStatusLevel(Level.WARN)
    builder.setConfigurationName("FdSwarmLogging")

    val jsonTemplate =
      LogEventFieldNames.log4jEventTemplateWithFlattenedMdc

    val consoleLayout =
      builder
        .newLayout("JsonTemplateLayout")
        .addAttribute("eventTemplate", jsonTemplate)
        .addAttribute("eventEol", true)

    val fileLayout =
      builder
        .newLayout("JsonTemplateLayout")
        .addAttribute("eventTemplate", jsonTemplate)
        .addAttribute("eventEol", true)

    val console =
      builder.newAppender("Console", "Console")
    console.addAttribute("target", "SYSTEM_OUT")
    console.add(consoleLayout)
    builder.add(console)

    val rollingFile =
      builder.newAppender("File", "RollingFile")
    rollingFile.addAttribute("fileName", logFile.toString)
    rollingFile.addAttribute("filePattern", logPattern.toString)
    rollingFile.add(fileLayout)

    val policies = builder.newComponent("Policies")
    policies.addComponent(
      builder
        .newComponent("TimeBasedTriggeringPolicy")
        .addAttribute("interval", 1)
    )
    rollingFile.addComponent(policies)
    builder.add(rollingFile)

    val accessFile =
      builder.newAppender("AccessFile", "File")
    accessFile.addAttribute("fileName", accessLogFile.toString)
    accessFile.addAttribute("immediateFlush", true)
    accessFile.add(
      builder
        .newLayout("PatternLayout")
        .addAttribute("pattern", "%msg%n")
    )
    builder.add(accessFile)

    builder.add(
      builder
        .newLogger("org.http4s.server.middleware.Logger", Level.INFO)
        .addAttribute("additivity", false)
        .add(builder.newAppenderRef("AccessFile"))
    )

    builder.add(
      builder
        .newRootLogger(Level.INFO)
        .add(builder.newAppenderRef("Console"))
        .add(builder.newAppenderRef("File"))
    )

    Configurator.reconfigure(builder.build())
