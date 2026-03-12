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

import munit.FunSuite
import scalafx.beans.property.StringProperty
import scalafx.Includes._

class MultiChangeWatcherTest extends FunSuite:

  test("MultiChangeWatcher should trigger on every change"):
    val prop1 = StringProperty("a")
    val prop2 = StringProperty("b")
    val watcher = MultiChangeWatcher(prop1, prop2)
    
    var changeCount = 0
    watcher.onChange { (_, _, _) =>
      changeCount += 1
    }
    
    prop1.value = "a1"
    assertEquals(changeCount, 1, "First change should trigger")
    
    prop2.value = "b1"
    assertEquals(changeCount, 2, "Second change should trigger")
    
    prop1.value = "a2"
    assertEquals(changeCount, 3, "Third change should trigger")
