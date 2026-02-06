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
import scalafx.scene.control.Label
import scalafx.scene.layout.{GridPane, StackPane, BorderPane}

class GridUtilsTest extends FunSuite:

  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  test("toGrid arranges items in rows and columns") {
    val items = (1 to 8).map(i => new Label(i.toString))
    val nCols = 3
    val grid = GridUtils.toGrid(items, nCols)

    // Layout should be (nCols = 3):
    // (0,0) (0,1) (0,2)  <- row 0
    // (1,0) (1,1) (1,2)  <- row 1
    // (2,0) (2,1)        <- row 2
    
    // (row, col):
    // 1: (0,0), 2: (0,1), 3: (0,2)
    // 4: (1,0), 5: (1,1), 6: (1,2)
    // 7: (2,0), 8: (2,1)

    def getCoord(idx: Int): (Int, Int) =
      (idx / nCols, idx % nCols)

    items.zipWithIndex.foreach { case (item, idx) =>
      val (expectedRow, expectedCol) = getCoord(idx)
      assertEquals(GridPane.getRowIndex(item).toInt, expectedRow)
      assertEquals(GridPane.getColumnIndex(item).toInt, expectedCol)
    }
  }

  test("fieldSet creates a StackPane with a Label and content") {
    val content = new Label("Content")
    val pane = GridUtils.fieldSet("Title", content)
    assert(pane.isInstanceOf[StackPane])
    
    // Debug: print children classes
    // pane.children.foreach(c => println(s"[DEBUG_LOG] child: ${c.getClass.getName}"))
    
    // In ScalaFX/JavaFX, the classes might be the underlying JavaFX ones or ScalaFX wrappers
    assert(pane.children.exists(c => c.isInstanceOf[javafx.scene.control.Label] || c.isInstanceOf[Label]))
    assert(pane.children.exists(c => c.isInstanceOf[javafx.scene.layout.BorderPane] || c.isInstanceOf[BorderPane]))
  }
