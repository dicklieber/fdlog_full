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

import munit.FunSuite
import fdswarm.io.DirectoryProvider
import org.apache.logging.log4j.LogManager
import java.nio.file.Files
import scala.jdk.CollectionConverters._

class LoggingReproductionTest extends FunSuite {
  test("logging to file should work after LoggingConfigurator.addFileAppender") {
    // PRE-INITIALIZE Log4j
    LogManager.getLogger("fdswarm.PreInit").info("Pre-initialization")

    val tempDir = Files.createTempDirectory("fdswarm-log-test")
    val directoryProvider: DirectoryProvider = () => os.Path(tempDir.toAbsolutePath.toString)
    
    // Configure logging
    LoggingConfigurator.addFileAppender(directoryProvider)
    
    val log = LogManager.getLogger("fdswarm.StationManager")
    val message = "TEST LOG MESSAGE " + System.currentTimeMillis()
    log.info(message)
    
    val accessLog = LogManager.getLogger("org.http4s.server.middleware.Logger")
    val accessMessage = "ACCESS LOG MESSAGE " + System.currentTimeMillis()
    accessLog.info(accessMessage)
    
    val otherLog = LogManager.getLogger("fdswarm.SomeOtherClass")
    val otherMessage = "OTHER LOG MESSAGE " + System.currentTimeMillis()
    otherLog.info(otherMessage)

    // Force a wait or flush if possible? Log4j usually writes fairly quickly.
    Thread.sleep(500)
    
    val logFile = tempDir.resolve("fdswarm.log")
    assert(Files.exists(logFile), s"Log file $logFile should exist")
    
    val lines = Files.readAllLines(logFile).asScala
    assert(lines.exists(_.contains(message)), s"Log file should contain the message: '$message'. Content: ${lines.mkString("\n")}")

    val accessLogFile = tempDir.resolve("access.log")
    assert(Files.exists(accessLogFile), s"Access log file $accessLogFile should exist")
    val accessLines = Files.readAllLines(accessLogFile).asScala
    assert(accessLines.exists(_.contains(accessMessage)), s"Access log file should contain the message: '$accessMessage'. Content: ${accessLines.mkString("\n")}")
  }

  test("TRACE logging should work after Setting level via LoggingManager") {
    val tempDir = Files.createTempDirectory("fdswarm-trace-test")
    val directoryProvider: DirectoryProvider = () => os.Path(tempDir.toAbsolutePath.toString)

    // 1. Configure logging
    LoggingConfigurator.addFileAppender(directoryProvider)

    // 2. Use LoggingManager to set level to TRACE
    val loggingManager = new LoggingManager(directoryProvider)
    val loggerName = "fdswarm.util.HostAndPortProvider"
    loggingManager.updateLogger(loggerName, LevelEnum.TRACE)

    // 3. Log at TRACE level
    val log = org.slf4j.LoggerFactory.getLogger(loggerName)
    val traceMessage = "TRACE TEST MESSAGE " + System.currentTimeMillis()
    log.trace(traceMessage)

    // 4. Verify it's in the log file
    Thread.sleep(500)
    val logFile = tempDir.resolve("fdswarm.log")
    val lines = Files.readAllLines(logFile).asScala
    assert(lines.exists(_.contains(traceMessage)), s"Log file should contain TRACE message. Content: ${lines.mkString("\n")}")
  }
}
