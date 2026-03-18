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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.DebugConfig
import fdswarm.io.DirectoryProvider
import fdswarm.util.Ids.Id
import jakarta.inject.Inject
import os.SubProcess

import scala.collection.mutable

/** create a JSON file of [[DebugConfig]] Starts an instance of the FDSwarm
  * application. pass reference to that file on the command line.
  *
  * @param directoryProvider
  */
class Runner @Inject() (directoryProvider: DirectoryProvider)
    extends LazyLogging:

  private val managedNodes = mutable.Map.empty[Id, ManagedNode]

  def start(debugConfig: DebugConfig): Unit =
    managedNodes
      .getOrElseUpdate(
        debugConfig.id,
        ManagedNode(debugConfig, directoryProvider, os.pwd / os.RelPath(AppInstance.jarPath))
      )
      .start()

  def stop(): Unit =
    managedNodes.values.foreach(_.stop())
    managedNodes.clear()


