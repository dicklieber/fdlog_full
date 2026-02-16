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

package fdswarm.replication

import com.organization.BuildInfo
import munit.FunSuite
import scala.util.Success
import scala.util.Failure

import upickle.default.*
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class UDPHeaderTest extends FunSuite:

  test("UDPHeader generates correct Status header"):
    val bytes = UDPHeader(Service.Status)
    val expected = s"FDSWARM|Status|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    assert(bytes.sameElements(expected))

  test("UDPHeader generates correct Discovery header"):
    val bytes = UDPHeader(Service.Discovery)
    val expected = s"FDSWARM|Discovery|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    assert(bytes.sameElements(expected))

  test("UDPHeader generates correct header with payload"):
    val payload = "Hello World".getBytes("UTF-8")
    val bytes = UDPHeader(Service.Status, payload)
    val headerPart = s"FDSWARM|Status|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val expected = new Array[Byte](headerPart.length + payload.length)
    System.arraycopy(headerPart, 0, expected, 0, headerPart.length)
    System.arraycopy(payload, 0, expected, headerPart.length, payload.length)
    assert(bytes.sameElements(expected))

  test("UDPHeader.parse correctly parses valid Discovery header with Unit"):
    val header = s"FDSWARM|Discovery|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val result = UDPHeader.parse(header)
    assertEquals(result.service, Service.Discovery)
    assert(result.payload.isEmpty)

  test("UDPHeader.parse correctly parses valid Status header with String payload"):
    val jsonPayload = "\"status-ok\""
    val header = (s"FDSWARM|Status|${BuildInfo.dataVersion}|\n" + jsonPayload).getBytes("UTF-8")
    val result = UDPHeader.parse(header)
    assertEquals(result.service, Service.Status)
    assertEquals(new String(result.payload, "UTF-8"), jsonPayload)

  test("UDPHeader.parse correctly parses gzipped payload"):
    val jsonPayload = "\"gzipped-payload\""
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos)
    gzos.write(jsonPayload.getBytes("UTF-8"))
    gzos.close()
    val gzippedPayload = baos.toByteArray
    
    val headerPart = s"FDSWARM|Status|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val packet = new Array[Byte](headerPart.length + gzippedPayload.length)
    System.arraycopy(headerPart, 0, packet, 0, headerPart.length)
    System.arraycopy(gzippedPayload, 0, packet, headerPart.length, gzippedPayload.length)
    
    val result = UDPHeader.parse(packet)
    assertEquals(result.service, Service.Status)
    assert(result.payload.sameElements(gzippedPayload))

  test("UDPHeader.parse fails on invalid prefix"):
    val header = s"INVALID|Status|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    intercept[IllegalArgumentException](UDPHeader.parse(header))

  test("UDPHeader.parse fails on invalid version"):
    val header = s"FDSWARM|Status|999|\n".getBytes("UTF-8")
    intercept[IllegalArgumentException](UDPHeader.parse(header))

  test("UDPHeader.parse fails on unknown service"):
    val header = s"FDSWARM|Unknown|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    intercept[IllegalArgumentException](UDPHeader.parse(header))
