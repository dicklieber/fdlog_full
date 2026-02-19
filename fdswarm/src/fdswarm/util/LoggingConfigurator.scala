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
import org.apache.logging.log4j.core.LoggerContext

object LoggingConfigurator:
  def addFileAppender(directoryProvider: DirectoryProvider): Unit =
    val logDir = directoryProvider()
    val logFile = logDir / "fdswarm.log"
    val accessLogFile = logDir / "access.log"
    System.setProperty("fdlog.logFile", logFile.toString())
    System.setProperty("fdlog.accessLogFile", accessLogFile.toString())
    val context = LoggerContext.getContext(false)
    context.reconfigure()
