/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.util

import fdswarm.io.DirectoryProvider
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.config.Configurator

object LoggingConfigurator:
  def addFileAppender(directoryProvider: DirectoryProvider): Unit =
    val logDir = directoryProvider()
    val logFile = logDir / "fdswarm.log"
    val accessLogFile = logDir / "access.log"

    val builder = ConfigurationBuilderFactory.newConfigurationBuilder()
    builder.setStatusLevel(Level.WARN)

    // Console Appender
    val console = builder.newAppender("TestConsole", "Console")
    console.addAttribute("target", "SYSTEM_ERR")
    console.add(builder.newLayout("PatternLayout")
      .addAttribute("pattern", "%logger{36} %highlight{%-5level}{FATAL=bg_red, ERROR=red, WARN=yellow, INFO=green, info=blue, info=cyan}  %msg%n"))
    builder.add(console)

    // File Appender
    val file = builder.newAppender("FileAppender", "File")
    file.addAttribute("fileName", logFile.toString())
    file.addAttribute("immediateFlush", true)
    file.add(builder.newLayout("PatternLayout")
      .addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"))
    builder.add(file)

    // Access Log Appender
    val accessFile = builder.newAppender("AccessLogAppender", "File")
    accessFile.addAttribute("fileName", accessLogFile.toString())
    accessFile.addAttribute("immediateFlush", true)
    accessFile.add(builder.newLayout("PatternLayout")
      .addAttribute("pattern", "%msg%n"))
    builder.add(accessFile)

    // Loggers
    val loggers = Seq(
      "fdlog.store.QsoStore",
      "fdswarm.fx.qso.QsoEntryPanel",
      "fdswarm.StationManager",
      "fdswarm.fx.ConfigModule",
      "fdswarm.replication.NodeStatusService",
      "fdswarm.store.QsoStore",
      "fdswarm.replication.NodeStatusSender",
      "fdswarm.fx.tools.FdHourDialogService",
      "fdswarm.api.QsoRoutes",
      "fdswarm.replication.NodeStatusHandler"
    )

    loggers.foreach { name =>
      builder.add(builder.newLogger(name, Level.INFO).addAttribute("additivity", true))
    }

    // Special logger for Access Log
    builder.add(builder.newLogger("org.http4s.server.middleware.Logger", Level.INFO)
      .addAttribute("additivity", false)
      .add(builder.newAppenderRef("AccessLogAppender")))

    // Root Logger
    builder.add(builder.newRootLogger(Level.INFO)
      .add(builder.newAppenderRef("TestConsole"))
      .add(builder.newAppenderRef("FileAppender")))

    Configurator.reconfigure(builder.build())
