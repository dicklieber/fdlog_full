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
import munit.FunSuite
import scalafx.scene.control.Label
import scalafx.scene.layout.{GridPane, StackPane, BorderPane}

class GridColumnsTest extends FunSuite:
  test("toGrid arranges items in rows and columns"):
    val items = (1 to 8).map(
      i => new Label(i.toString)
    )
    val nCols = 3
    val grid = GridColumns.toGrid(
      items,
      nCols
    )
    def getCoord(idx: Int): (Int, Int) =
      (idx / nCols, idx % nCols)
    items.zipWithIndex.foreach {
      case (item, idx) =>
        val (expectedRow, expectedCol) = getCoord(idx)
        assertEquals(
          GridPane.getRowIndex(item).toInt,
          expectedRow
        )
        assertEquals(
          GridPane.getColumnIndex(item).toInt,
          expectedCol
        )
    }
    assert(
      grid != null
    )

  test("fieldSet creates a StackPane with a Label and content"):
    val content = new Label(
      "Content"
    )
    val pane = GridColumns.fieldSet(
      "Title",
      content
    )
    assert(
      pane.isInstanceOf[StackPane]
    )
    assert(
      pane.children.exists(
        _.isInstanceOf[javafx.scene.control.Label]
      )
    )
    assert(
      pane.children.exists(
        _.isInstanceOf[javafx.scene.layout.BorderPane]
      )
    )
