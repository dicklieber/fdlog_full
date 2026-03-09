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
import fdswarm.io.DirectoryProvider
import io.circe.syntax.*

class InstanceIdManagerTest extends FunSuite:

  test("instanceId is persisted and reloaded from DirectoryProvider"):
    val tmpDir = os.temp.dir()
    val provider = new DirectoryProvider:
      def apply(): os.Path = tmpDir

    // First initialization
    val manager1 = new InstanceIdManager(provider)
    val id1 = manager1.ourInstanceId
    assert(id1.nonEmpty)
    assert(os.exists(tmpDir / "instance.json"))
    
    // Check content of JSON
    val savedJson = os.read(tmpDir / "instance.json")
    assert(savedJson.contains(id1))
    assert(savedJson.contains("instanceId"))

    // Second initialization (reloading)
    val manager2 = new InstanceIdManager(provider)
    val id2 = manager2.ourInstanceId
    assertEquals(id1, id2)

    // Modification of the file
    val customConfig = InstanceConfig("custom-id")
    os.write.over(tmpDir / "instance.json", customConfig.asJson.noSpaces)
    val manager3 = new InstanceIdManager(provider)
    assertEquals(manager3.ourInstanceId, "custom-id")

  test("generates new ID if JSON is invalid"):
    val tmpDir = os.temp.dir()
    val provider = new DirectoryProvider:
      def apply(): os.Path = tmpDir

    os.write.over(tmpDir / "instance.json", "invalid json")
    val manager = new InstanceIdManager(provider)
    assert(manager.ourInstanceId.nonEmpty)
    assert(manager.ourInstanceId != "invalid json")
