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
import fdswarm.fx.NamedValueCollector
import scalafx.Includes.*
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.GridPane

import scala.collection.concurrent.TrieMap

class GridBuilder:
  val grid = new GridPane()
  val kvPairs = new TrieMap[String, Any]()
  private var row = 0
  def apply(key:String, value:Any):GridPane=
    kvPairs.put(key, value.toString)
    grid.add(new Label(key), 0, row)
    grid.add(GridBuilder.label(value), 1, row)
    row += 1
    grid
    

object GridBuilder:
  /**
   * Builds a [[Label]] from the given value.
   */
  def label(value:Any):Node=
    value match
      case node:Node => 
        node
        // add more specific types here
      case x =>
        new Label(x.toString)
    