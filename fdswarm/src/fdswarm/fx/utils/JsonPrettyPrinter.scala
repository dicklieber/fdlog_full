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
import io.circe.*
import io.circe.parser.*

object JsonPrettyPrinter:

  /**
   * Pretty-prints JSON with syntax coloring in a TextFlow.
   * @param json the JSON string to pretty-print
   * @return a TextFlow containing the colored JSON
   */
  def colorize(json: String): Pane =
    try
      val data = if json.trim.startsWith("{") || json.trim.startsWith("[") then
        parse(json).getOrElse(throw new Exception("Invalid JSON"))
      else
        // Might be NDJSON (multiple lines of JSON)
        val lines = json.split("\n").filter(_.trim.nonEmpty)
        if lines.length > 1 then
          Json.arr(lines.map(line => parse(line).getOrElse(throw new Exception("Invalid JSON line")))*)
        else
          parse(json).getOrElse(throw new Exception("Invalid JSON"))

      val textFlow = new TextFlow():
        styleClass.add("json-pretty-print")
      renderValue(data, 0, textFlow)
      textFlow
    catch
      case e: Exception =>
        val flow = new TextFlow():
          styleClass.add("fixed-width")
        flow.children.add(new Text(s"Invalid JSON: ${e.getMessage}") { fill = Color.Red; styleClass.add("fixed-width") })
        flow

  private def renderValue(value: Json, indent: Int, flow: TextFlow): Unit =
    value.fold(
      flow.children.add(new Text("null") { fill = Color.Gray; styleClass.add("fixed-width") }),
      b => flow.children.add(new Text(b.toString) { fill = Color.Orange; styleClass.add("fixed-width") }),
      n => flow.children.add(new Text(n.toString) { fill = Color.Blue; styleClass.add("fixed-width") }),
      s => flow.children.add(new Text(s"\"$s\"") { fill = Color.Green; styleClass.add("fixed-width") }),
      arr => {
        flow.children.add(new Text("[") { styleClass.add("fixed-width") })
        if arr.nonEmpty then
          flow.children.add(new Text("\n") { styleClass.add("fixed-width") })
          arr.zipWithIndex.foreach { (v, i) =>
            flow.children.add(new Text("  " * (indent + 1)) { styleClass.add("fixed-width") })
            renderValue(v, indent + 1, flow)
            if i < arr.size - 1 then flow.children.add(new Text(",") { styleClass.add("fixed-width") })
            flow.children.add(new Text("\n") { styleClass.add("fixed-width") })
          }
          flow.children.add(new Text("  " * indent) { styleClass.add("fixed-width") })
        flow.children.add(new Text("]") { styleClass.add("fixed-width") })
      },
      obj => {
        flow.children.add(new Text("{") { styleClass.add("fixed-width") })
        if obj.nonEmpty then
          flow.children.add(new Text("\n") { styleClass.add("fixed-width") })
          val items = obj.toList
          items.zipWithIndex.foreach { case ((k, v), i) =>
            flow.children.add(new Text("  " * (indent + 1)) { styleClass.add("fixed-width") })
            flow.children.add(new Text(s"\"$k\"") { fill = Color.Purple; styleClass.add("fixed-width") })
            flow.children.add(new Text(": ") { styleClass.add("fixed-width") })
            renderValue(v, indent + 1, flow)
            if i < items.size - 1 then flow.children.add(new Text(",") { styleClass.add("fixed-width") })
            flow.children.add(new Text("\n") { styleClass.add("fixed-width") })
          }
          flow.children.add(new Text("  " * indent) { styleClass.add("fixed-width") })
        flow.children.add(new Text("}") { styleClass.add("fixed-width") })
      }
    )
