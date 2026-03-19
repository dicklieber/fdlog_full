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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.Printer
import jakarta.inject.{Inject, Singleton}
import java.nio.file.Files

class LoggingManager @Inject() (directoryProvider: DirectoryProvider) extends LazyLogging:
  private val loggingJsonPath = directoryProvider() / "logging.json"
  private var currentLoggers: List[LoggerLevel] = load()


  def getLoggers: List[LoggerLevel] = currentLoggers

  def updateLogger(loggerName: String, level: LevelEnum): Unit =
    val index = currentLoggers.indexWhere(_.logger == loggerName)
    if (index >= 0) 
      currentLoggers = currentLoggers.updated(index, LoggerLevel(loggerName, level))
    else 
      currentLoggers = currentLoggers :+ LoggerLevel(loggerName, level)
    applyToLog4j2(loggerName, level)
    save()


  def removeAllLoggers(): Unit =
    currentLoggers.foreach(ll => Configurator.setLevel(ll.logger, Level.INFO))
    currentLoggers = List.empty
    save()

  private def applyToLog4j2(loggerName: String, level: LevelEnum): Unit =
    val l4jLevel = Level.toLevel(level.toString, Level.INFO)
    Configurator.setLevel(loggerName, l4jLevel)

  private def save(): Unit =
    val json = currentLoggers.asJson.printWith(Printer.indented("  "))
    os.write.over(loggingJsonPath, json, createFolders = true)

  private def load(): List[LoggerLevel] =
    if (os.exists(loggingJsonPath)) {
      try {
        val json = os.read(loggingJsonPath)
        decode[List[LoggerLevel]](json) match {
          case Right(loggers) => loggers
          case Left(error) =>
            System.err.println(s"Error decoding logging.json: ${error.getMessage}")
            defaultLoggers
        }
      } catch {
        case e: Exception =>
          System.err.println(s"Error loading logging.json: ${e.getMessage}")
          defaultLoggers
      }
    } else {
      defaultLoggers
    }

  private def defaultLoggers: List[LoggerLevel] = List.empty

  def applyInitialConfig(): Unit =
    if (currentLoggers.nonEmpty) {
      System.out.println("Logging configuration from logging.json:")
      currentLoggers.foreach { ll =>
        System.out.println(s"  ${ll.logger}: ${ll.level}")
        applyToLog4j2(ll.logger, ll.level)
      }
    } else {
      logger.debug("No logging configuration found in logging.json.")
    }
