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


import java.time.Instant

/**
 * Manages the fdswarm-all.jar assembly at out/fdswarm/assembly.dest/fdswarm-all.jar
 */
object FdswarmJarManager :

  private val rootDir: String = sys.props("user.dir")

  val jarPath: os.Path = os.Path(rootDir) / "out" / "fdswarm" / "assembly.dest" / "fdswarm-all.jar"

  def jarInfo(): Option[Instant] =
    if os.exists(jarPath) && os.isFile(jarPath) then
      Some(Instant.ofEpochMilli(os.mtime(jarPath)))
    else
      None

  def build(): Unit =
    val result = os.proc("./mill", "fdswarm.assembly").call(cwd = os.Path(rootDir))
    if result.exitCode != 0 then
      throw RuntimeException(s"Mill build failed with exit code ${result.exitCode}")
