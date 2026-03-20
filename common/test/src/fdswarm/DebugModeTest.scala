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

package fdswarm

import munit.FunSuite
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.parser._

class DebugModeTest extends FunSuite {

  test("Off.javaOpt returns None") {
    assertEquals(DebugMode.Off.javaOpt, None)
  }

  test("Debug.javaOpt returns correct agentlib option") {
    val expected = Some("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    assertEquals(DebugMode.Debug.javaOpt, expected)
  }

  test("Wait.javaOpt returns correct agentlib option") {
    val expected = Some("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
    assertEquals(DebugMode.Wait.javaOpt, expected)
  }

  test("Off round-trips through JSON") {
    val original = DebugMode.Off
    val jsonStr = original.asJson.noSpaces
    val parsed = parse(jsonStr).flatMap(_.as[DebugMode])
    assertEquals(parsed, Right(original))
  }

  test("Debug round-trips through JSON") {
    val original = DebugMode.Debug
    val jsonStr = original.asJson.noSpaces
    val parsed = parse(jsonStr).flatMap(_.as[DebugMode])
    assertEquals(parsed, Right(original))
  }

  test("Wait round-trips through JSON") {
    val original = DebugMode.Wait
    val jsonStr = original.asJson.noSpaces
    val parsed = parse(jsonStr).flatMap(_.as[DebugMode])
    assertEquals(parsed, Right(original))
  }
}