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

package fdswarm.util

import munit.FunSuite

class IdsTest extends FunSuite:

  test("generateId produces unique random IDs"):
    val id1 = Ids.generateId()
    val id2 = Ids.generateId()
    assert(id1 != id2)
    assertEquals(id1.length, Ids.IdSize)
    assertEquals(id2.length, Ids.IdSize)

  test("generateId produces sequential IDs when configured"):
    Ids.useSeqentialStartingAt(100)
    try
      val id1 = Ids.generateId()
      val id2 = Ids.generateId()
      assertEquals(id1, "100")
      assertEquals(id2, "101")
    finally
      Ids.revertToRandom()

  test("revertToRandom switches back to random IDs"):
    Ids.useSeqentialStartingAt(500)
    Ids.revertToRandom()
    val id = Ids.generateId()
    assert(id != "500")
    assertEquals(id.length, Ids.IdSize)

  test("generateInstanceId is exactly three characters"):
    val instanceId = Ids.generateInstanceId()
    println(s"[DEBUG_LOG] generateInstanceId: $instanceId length: ${instanceId.length}")
    assertEquals(instanceId.length, 3)

  test("generateInstanceId produces unique-ish IDs"):
    val ids = (1 to 100).map(_ => Ids.generateInstanceId()).toSet
    assert(ids.size > 90) // Allow for some collisions in a small sample of 16-bit space
