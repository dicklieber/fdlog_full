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

import fdswarm.JavaFxTestKit
import munit.FunSuite
import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.scene.control.Label
import scalafx.scene.layout.GridPane

class GridBuilderTest extends FunSuite:

  test("GridBuilder builds a GridPane with correct rows"):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      builder("Name:", "John")
      builder("Age:", 30)
      val nameProperty = StringProperty("Engineer")
      builder("Job:", nameProperty)
      
      val grid: GridPane = builder.result
      
      assertEquals(grid.hgap.value, 10.0)
      assertEquals(grid.vgap.value, 2.0)
      
      // Check labels
      def getLabelAt(row: Int, col: Int): Label =
        grid.children.find(n => GridPane.getRowIndex(n) == row && GridPane.getColumnIndex(n) == col)
          .map {
            case l: javafx.scene.control.Label => new Label(l)
            case _ => fail(s"Node at $row, $col is not a label")
          }
          .getOrElse(fail(s"No label at $row, $col"))

      assertEquals(getLabelAt(0, 0).text.value, "Name:")
      assertEquals(getLabelAt(0, 1).text.value, "John")
      
      assertEquals(getLabelAt(1, 0).text.value, "Age:")
      assertEquals(getLabelAt(1, 1).text.value, "30")
      
      assertEquals(getLabelAt(2, 0).text.value, "Job:")
      assertEquals(getLabelAt(2, 1).text.value, "Engineer")
      
      nameProperty.value = "Senior Engineer"
      assertEquals(getLabelAt(2, 1).text.value, "Senior Engineer")
    }
