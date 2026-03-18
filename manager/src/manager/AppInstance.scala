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
import manager.AppInstance.jarPath
import os.SubProcess

class AppInstance(debugConfigJsonPath: String, port: Int) extends LazyLogging:
  val proc =
    os.proc("java", "-jar", jarPath, s"startupInfo=$debugConfigJsonPath")
  val subProcess: SubProcess = proc.spawn(env = Map("PORT" -> port.toString))
  logger.trace(s"Started $proc s: $subProcess")

  def stop(): Unit =
    subProcess.destroy()
object AppInstance:
  val jarPath: String = "out/fdswarm/assembly.dest/fdswarm-all.jar"
