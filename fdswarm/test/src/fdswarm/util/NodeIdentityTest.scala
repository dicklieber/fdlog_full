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
import io.circe.syntax.*
import io.circe.parser.decode

class NodeIdentityTest extends FunSuite:

  test("string round trip"):
    val nodeIdentity = NodeIdentity(name =)
    val string = nodeIdentity.toString
    val backAgain = NodeIdentity(string)
    assertEquals( backAgain, nodeIdentity)

  test("circe round trip"):
    val nodeIdentity = NodeIdentity(name =)
    val json = nodeIdentity.asJson.noSpaces
    val decoded = decode[NodeIdentity](json)
      .getOrElse(fail("failed to decode"))
    assertEquals(decoded, nodeIdentity)

  test("handle legacy 'local' string"):
    val nodeIdentity = NodeIdentity("local")
    assertEquals(nodeIdentity, NodeIdentity(name =))

  test("PortAndInstance circe round trip"):
    val portAndInstance = PortAndInstance(8080, "test-instance")
    val json = portAndInstance.asJson.noSpaces
    println(json)
    val decoded = decode[PortAndInstance](json)
      .getOrElse(fail("failed to decode"))
    assertEquals(decoded, portAndInstance)

