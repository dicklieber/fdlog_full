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

/**
 * 
 * @param path to display.
 * @param prefix just for internal recursive use.
 * @param isLast just for internal recursive use.
 */
def printTreePretty(path: os.Path, prefix: String = "", isLast: Boolean = true): Unit =
  val connector =
    if prefix.isEmpty then ""
    else if isLast then "└── "
    else "├── "

  println(prefix + connector + path.last)

  if os.isDir(path) then
    val children = os.list(path).sortBy(_.last)
    val newPrefix =
      if prefix.isEmpty then ""
      else if isLast then prefix + "    "
      else prefix + "│   "

    for (child, idx) <- children.zipWithIndex do
      val last = idx == children.size - 1
      printTreePretty(child, newPrefix, last)