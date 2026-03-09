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

import scalafx.beans.binding.Bindings
import scalafx.beans.value.ObservableValue
import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.layout.GridPane
import scalafx.Includes.*

/**
 * A builder for creating a GridPane with label-value pairs.
 */
class GridBuilder:
  private val grid = new GridPane {
    hgap = 10
    vgap = 2
    padding = Insets(5)
  }
  private var rowIdx = 0

  def apply(label: String, value: Any): GridBuilder =
    val lLabel = new Label(label) {
      style = "-fx-font-weight: bold;"
    }
    grid.add(lLabel, 0, rowIdx)

    value match
      case obs: ObservableValue[?, ?] =>
        val vLabel = new Label()
        obs match
          case sObs: ObservableValue[String, String] @unchecked =>
            vLabel.text <== sObs
          case _ =>
            vLabel.text <== Bindings.createStringBinding(() => {
              val v = obs.value
              if v == null then "" else v.toString
            }, obs)

        grid.add(vLabel, 1, rowIdx)

      case _ =>
        val vLabel = new Label(value.toString)
        grid.add(vLabel, 1, rowIdx)

    rowIdx += 1
    this

  def result: GridPane = grid

object GridBuilder:
  def apply(): GridBuilder = new GridBuilder()
