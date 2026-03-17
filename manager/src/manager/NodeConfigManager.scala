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

import fdswarm.DebugConfig
import fdswarm.util.Ids.Id
import io.circe.Printer
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableBuffer

@Singleton
class NodeConfigManager @Inject()():
  private val file = os.home / "fdswarm" / "nodes.json"
  private val printer = Printer.spaces2.copy(dropNullValues = true)

  val observableBuffer: ObservableBuffer[DebugConfig] =
    val initial = load()
    val buffer = ObservableBuffer.from(initial)
    buffer.onChange { (_, _) =>
      persist()
    }
    buffer

  def add(debugConfig: DebugConfig): Unit =
    observableBuffer += debugConfig

  def delete(id: Id): Unit =
    observableBuffer.removeIf(_.id == id)

  def persist(): Unit =
    val json = printer.print(observableBuffer.toList.asJson)
    os.write.over(file, json, createFolders = true)

  private def load(): List[DebugConfig] =
    try
      if os.exists(file) then
        val json = os.read(file)
        decode[List[DebugConfig]](json) match
          case Right(list) => list
          case Left(err) =>
            System.err.println(s"Failed to decode NodeConfigs from $file: $err")
            Nil
      else Nil
    catch
      case e: Exception =>
        System.err.println(s"Failed to load NodeConfigs from $file: ${e.getMessage}")
        Nil
