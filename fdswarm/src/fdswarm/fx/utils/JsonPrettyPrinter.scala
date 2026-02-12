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

package fdswarm.fx.utils

import scalafx.scene.layout.Pane
import scalafx.scene.text.{Text, TextFlow}
import scalafx.scene.paint.Color
import upickle.default.*
import ujson.Value

object JsonPrettyPrinter:

  /**
   * Pretty-prints JSON with syntax coloring in a TextFlow.
   * @param json the JSON string to pretty-print
   * @return a TextFlow containing the colored JSON
   */
  def colorize(json: String): Pane =
    try
      val data = if json.trim.startsWith("{") || json.trim.startsWith("[") then
        ujson.read(json)
      else
        // Might be NDJSON (multiple lines of JSON)
        val lines = json.split("\n").filter(_.trim.nonEmpty)
        if lines.length > 1 then
          ujson.Arr(lines.map(line => ujson.read(line))*)
        else
          ujson.read(json)

      val textFlow = new TextFlow()
      renderValue(data, 0, textFlow)
      textFlow
    catch
      case e: Exception =>
        val flow = new TextFlow()
        flow.children.add(new Text(s"Invalid JSON: ${e.getMessage}") { fill = Color.Red })
        flow

  private def renderValue(value: Value, indent: Int, flow: TextFlow): Unit =
    value match
      case ujson.Obj(items) =>
        flow.children.add(new Text("{"))
        if items.nonEmpty then
          flow.children.add(new Text("\n"))
          items.zipWithIndex.foreach { case ((k, v), i) =>
            flow.children.add(new Text("  " * (indent + 1)))
            flow.children.add(new Text(s"\"$k\"") { fill = Color.Purple })
            flow.children.add(new Text(": "))
            renderValue(v, indent + 1, flow)
            if i < items.size - 1 then flow.children.add(new Text(","))
            flow.children.add(new Text("\n"))
          }
          flow.children.add(new Text("  " * indent))
        flow.children.add(new Text("}"))

      case ujson.Arr(items) =>
        flow.children.add(new Text("["))
        if items.nonEmpty then
          flow.children.add(new Text("\n"))
          items.zipWithIndex.foreach { (v, i) =>
            flow.children.add(new Text("  " * (indent + 1)))
            renderValue(v, indent + 1, flow)
            if i < items.size - 1 then flow.children.add(new Text(","))
            flow.children.add(new Text("\n"))
          }
          flow.children.add(new Text("  " * indent))
        flow.children.add(new Text("]"))

      case ujson.Str(s) =>
        flow.children.add(new Text(s"\"$s\"") { fill = Color.Green })

      case ujson.Num(n) =>
        flow.children.add(new Text(n.toString) { fill = Color.Blue })

      case ujson.Bool(b) =>
        flow.children.add(new Text(b.toString) { fill = Color.Orange })

      case ujson.Null =>
        flow.children.add(new Text("null") { fill = Color.Gray })
