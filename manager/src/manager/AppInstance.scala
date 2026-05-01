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

import fdswarm.logging.LazyStructuredLogging
import manager.AppInstance.jarPath
import os.SubProcess
import fdswarm.StartupConfig

class AppInstance(debugConfigJsonPath: String,
                  startupConfig: StartupConfig,
                  port: Int) extends LazyStructuredLogging:
  private val debugOpt: Option[String] = startupConfig.debugMode.javaOpt
  private val logDir = os.home / "fdswarm" / port.toString
  private val stdoutLog = logDir / "stdout.log"
  private val stderrLog = logDir / "stderr.log"
  os.makeDir.all(logDir)

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
  val subProcess: SubProcess = proc.spawn(
    env = Map("PORT" -> port.toString),
    stdout = os.PathRedirect(stdoutLog),
    stderr = os.PathRedirect(stderrLog)
  )
  private val processHandle = subProcess.wrapped.toHandle

  def stop(): Unit =
    if processHandle.isAlive then
      logger.info(s"Stopping instance ${startupConfig.id} pid=${processHandle.pid()}")

      // Kill descendants first to avoid orphan helper processes.
      processHandle.descendants().forEach { child =>
        if child.isAlive then child.destroyForcibly()
      }
      processHandle.destroyForcibly()
object AppInstance:
  val jarPath: String = "out/fdswarm/assembly.dest/fdswarm-all.jar"
