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

import _root_.io.circe.syntax.*
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StartupConfig
import fdswarm.io.DirectoryProvider
import jakarta.inject.Inject

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.IndexedSeqView

/** create a JSON file of [[StartupConfig]] Starts an instance of the FDSwarm
  * application. pass reference to that file on the command line.
  *
  * @param directoryProvider where manager puts it's files.
  */
class Runner @Inject() (directoryProvider: DirectoryProvider)
    extends LazyLogging:

  private var instances:Seq[AppInstance] = Seq.empty
  private val path = directoryProvider() / "debugConfigs"

  def start(view: IndexedSeqView[StartupConfig]): Unit =
    os.remove.all(path)
    val ports = new AtomicInteger(8080)
    instances = view.iterator.map { startupConfig =>
      val pathToJson = path / s"${startupConfig.id}.json"
      os.write.over(pathToJson, startupConfig.asJson.spaces2, createFolders = true)
      val sJsonPath = pathToJson.toString
      AppInstance(sJsonPath, ports.getAndIncrement())
    }.toSeq

  def stop(): Unit =
    instances.foreach(_.stop())


