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

package manager

import fdswarm.StartupConfig
import fdswarm.util.Ids.Id
import _root_.io.circe.Printer
import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.decode
import _root_.io.circe.syntax.*
import fdswarm.logging.LazyStructuredLogging
import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableBuffer

@Singleton
class NodeConfigManager @Inject()(directoryProvider:DirectoryProvider) extends LazyStructuredLogging:
  private val file = directoryProvider() / "nodes.json"
  private val printer = Printer.spaces2.copy(dropNullValues = true)

  val observableBuffer: ObservableBuffer[StartupConfig] =
    val initial = load()
    val buffer = ObservableBuffer.from(initial)
    buffer

  def add(debugConfig: StartupConfig): Unit =
    logger.trace(s"Adding NodeConfig: $debugConfig")
    observableBuffer += debugConfig

  def delete(id: Id): Unit =
    logger.trace(s"Deleting NodeConfig with id: $id")
    observableBuffer.removeIf(_.id == id)

  def persist(): Unit =
    logger.trace(s"Persisting NodeConfigs to $file")
    val json = printer.print(observableBuffer.toList.asJson)
    os.write.over(file, json, createFolders = true)

  private def load(): List[StartupConfig] =
    logger.trace(s"Loading NodeConfigs from $file")
    try
      if os.exists(file) then
        val json = os.read(file)
        decode[List[StartupConfig]](json) match
          case Right(list) => list
          case Left(err) =>
            logger.error(s"Failed to decode NodeConfigs from $file: $err")
            Nil
      else Nil
    catch
      case e: Exception =>
        logger.error(s"Failed to load NodeConfigs from $file: ${e.getMessage}")
        Nil
