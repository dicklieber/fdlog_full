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

package fdswarm.fx

import fdswarm.fx.GridBuilder.valueToLabel
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.GridPane

import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.Locale

/**
 * A builder for creating a GridPane with label-value pairs.
 *
 * @param header top line of the grid
 */
class GridBuilder(header: Option[String] = None):
  private val headerLabel = new Label() {
    styleClass += "grid-header"
    // will set col span later 
  }

  private val grid = new GridPane {
    hgap = 10
    vgap = 2
    padding = Insets(5)
  }
  private var maxValues = 0
  private val rowIdx = AtomicInteger()

  def this(header: String) =
    this(Option(header).filter(_.nonEmpty))

  header.foreach(h =>
    // Start with grid header, if any.
    headerLabel.text = h
    headerLabel.styleClass += "grid-row-label"
    grid.add(headerLabel, 0, rowIdx.getAndIncrement())
  )

  def apply(label: String, value: Any*): GridBuilder =
    maxValues = maxValues.max(value.length)
    val row = rowIdx.getAndIncrement()
    if (label.nonEmpty) {
      val rowLabel = new Label(label) {
        styleClass += "grid-row-label"
      }
      grid.add(rowLabel, 0, row)
    }

    value.zipWithIndex.foreach { case (value, idx) =>
      val valueValue = valueToLabel(value)
      grid.add(valueValue, idx + 1, row)
    }

    this

  def result: GridPane =
    if (header.isDefined) {
      GridPane.setColumnSpan(headerLabel, maxValues + 1)
    }
    grid

object GridBuilder:
  def apply(): GridBuilder = new GridBuilder()

  private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

  def valueToLabel(value: Any, style: String = "grid-value"): Node =
    value match
      case node: scalafx.scene.Node =>
        node // already a node, just use it.
      case s: String =>
        new Label(s)
      case prop: scalafx.beans.property.StringProperty =>
        new Label {
          text <== prop
        }
      case i: Int =>
        new Label(numberFormat.format(i)) {
          styleClass += "gridNumber"
        }
      case x =>
        new Label(x.toString)
