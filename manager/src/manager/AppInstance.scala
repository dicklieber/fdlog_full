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
import fdswarm.StartupConfig

class AppInstance(debugConfigJsonPath: String, startupConfig: StartupConfig, port: Int) extends LazyLogging:
  private val debugOpt: Option[String] = startupConfig.debugMode.javaOpt

  val args =
    Seq("java") ++
      debugOpt.toSeq ++
      Seq(
        "-jar",
        jarPath,
        "--startupInfo",
        debugConfigJsonPath
      )

  logger.debug(
    s"Command line that will be invoked: ${args.zipWithIndex.map((a, i) => s"[$i]='$a'").mkString(" ")}"
  )
  println()
  val proc = os.proc(args)
  val subProcess: SubProcess = proc.spawn(env = Map("PORT" -> port.toString))
  def stop(): Unit =
    subProcess.destroy()
object AppInstance:
  val jarPath: String = "out/fdswarm/assembly.dest/fdswarm-all.jar"
