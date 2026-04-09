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

  test("GridBuilder builds a GridPane with correct rows".ignore):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      builder.hgap = 10
      builder.vgap = 2
      builder("Name:", "John")
      builder("Age:", 1234)
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
      assertEquals(getLabelAt(1, 1).text.value, "1,234")
      
      assertEquals(getLabelAt(2, 0).text.value, "Job:")
      assertEquals(getLabelAt(2, 1).text.value, "Engineer")
      
      nameProperty.value = "Senior Engineer"
      assertEquals(getLabelAt(2, 1).text.value, "Senior Engineer")
      
    }

  test("GridBuilder handles Node as value".ignore):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      val customLabel = new Label("Custom Node Content")
      builder("Label:", customLabel)
      
      val grid: GridPane = builder.result
      
      // Check labels
      def getNodeAt(row: Int, col: Int): javafx.scene.Node =
        grid.children.find(n => GridPane.getRowIndex(n) == row && GridPane.getColumnIndex(n) == col)
          .getOrElse(fail(s"No node at $row, $col"))

      val node = getNodeAt(0, 1)
      assert(node.isInstanceOf[javafx.scene.control.Label])
      assertEquals(node.asInstanceOf[javafx.scene.control.Label].getText, "Custom Node Content")
      
      // If it just calls .toString on the customLabel, the text would be something else
      // (like "Label@...[styleClass=label]'Custom Node Content'")
    }

  test("GridBuilder handles empty label".ignore):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      builder("", "Value Only")
      
      val grid: GridPane = builder.result
      
      // There should be two children in this row: filler at col 0 and value at col 1
      val childrenInRow0 = grid.children.filter(n => GridPane.getRowIndex(n) == 0)
      assertEquals(childrenInRow0.size, 2)
      
      val filler = childrenInRow0.find(n => GridPane.getColumnIndex(n) == 0).get.asInstanceOf[javafx.scene.control.Label]
      assertEquals(filler.getText, "")
      
      val label = childrenInRow0.find(n => GridPane.getColumnIndex(n) == 1).get.asInstanceOf[javafx.scene.control.Label]
      assertEquals(label.getText, "Value Only")
    }

  test("GridBuilder handles header and column span".ignore):
    JavaFxTestKit.runOnFx {
      val builder = new GridBuilder("Test Header")
      builder("Label 1:", "Value 1", "Value 2")
      builder("Label 2:", "Only 1")

      val grid: GridPane = builder.result

      // Header should be at (0, 0)
      val headerNode = grid.children.find(n => GridPane.getRowIndex(n) == 0 && GridPane.getColumnIndex(n) == 0).get
      val headerLabel = headerNode.asInstanceOf[javafx.scene.control.Label]
      assertEquals(headerLabel.getText, "Test Header")

      // Max values is 2 (from Label 1). Total columns is 3. Column span should be 3.
      assertEquals(GridPane.getColumnSpan(headerNode).toInt, 3)
      assertEquals(headerLabel.getMaxWidth, Double.MaxValue)
      assertEquals(headerLabel.getMaxHeight, Double.MaxValue)
      assertEquals(headerLabel.getAlignment, javafx.geometry.Pos.CENTER)
    }

  test("GridBuilder fills empty cells in short rows".ignore):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      builder("Long Row:", "Val 1", "Val 2")
      builder("Short Row:", "Only 1")
      
      val grid = builder.result
      
      // Row 1 should have 3 children: label at 0, value at 1, and filler at 2
      val childrenInRow1 = grid.children.filter(n => GridPane.getRowIndex(n) == 1)
      assertEquals(childrenInRow1.size, 3)
      
      val filler = childrenInRow1.find(n => GridPane.getColumnIndex(n) == 2).get.asInstanceOf[javafx.scene.control.Label]
      assertEquals(filler.getText, "")
      assert(filler.getStyleClass.contains("grid-value"))
    }

  test("GridBuilder ensures style class for manually created labels".ignore):
    JavaFxTestKit.runOnFx {
      val builder = GridBuilder()
      val manualLabel = new Label("Manual")
      // manualLabel has no style class by default usually, 
      // but scalafx Label might have "label"
      builder("Test:", manualLabel)
      
      val grid = builder.result
      val label = grid.children.find(n => GridPane.getRowIndex(n) == 0 && GridPane.getColumnIndex(n) == 1).get.asInstanceOf[javafx.scene.control.Label]
      assert(label.getStyleClass.contains("grid-value"))
    }

  test("GridBuilder header does not have overlapping fillers".ignore):
    JavaFxTestKit.runOnFx {
      val builder = new GridBuilder("Overlap Test Header")
      builder("Row 1:", "Value 1", "Value 2") // maxValues = 2

      val grid: GridPane = builder.result
      
      // Header is at row 0. maxValues is 2. 
      // Total columns 0, 1, 2.
      // Header starts at col 0 and spans 3.
      // Filler loop should NOT add any more labels to row 0.
      val childrenInRow0 = grid.children.filter(n => GridPane.getRowIndex(n) == 0)
      
      assertEquals(childrenInRow0.size, 1, s"Row 0 should only have 1 child (header), found: ${childrenInRow0.map(n => s"at ${GridPane.getColumnIndex(n)} span ${GridPane.getColumnSpan(n)}").mkString(", ")}")
    }
