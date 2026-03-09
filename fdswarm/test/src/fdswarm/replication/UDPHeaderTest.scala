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
import fdswarm.util.PortAndInstance
import munit.FunSuite

import scala.util.Success
import scala.util.Failure
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.net.DatagramPacket
import java.net.InetAddress

class UDPHeaderTest extends FunSuite:

  test("UDPHeader generates correct Status header"):
    val pi = PortAndInstance(8080, "test-instance")
    val bytes = UDPHeader(Service.Status, pi)
    val expected = s"FDSWARM|Status|$pi|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    assert(bytes.sameElements(expected))

  test("UDPHeader generates correct header with payload"):
    val pi = PortAndInstance(8080, "test-instance")
    val payload = "Hello World".getBytes("UTF-8")
    val bytes = UDPHeader(Service.Status, pi, payload)
    val headerPart = s"FDSWARM|Status|$pi|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val expected = new Array[Byte](headerPart.length + payload.length)
    System.arraycopy(headerPart, 0, expected, 0, headerPart.length)
    System.arraycopy(payload, 0, expected, headerPart.length, payload.length)
    assert(bytes.sameElements(expected))

  test("UDPHeader.parse correctly parses valid Status header"):
    val instance = "other-instance"
    val port = 8081
    val pi = PortAndInstance(port, instance)
    val jsonPayload = "\"status-ok\""
    val headerData = (s"FDSWARM|Status|$pi|${BuildInfo.dataVersion}|\n" + jsonPayload).getBytes("UTF-8")
    val address = InetAddress.getByName("192.168.1.100")
    val packet = new DatagramPacket(headerData, headerData.length, address, 1234)
    
    val result = UDPHeader.parse(packet).get
    assertEquals(result.service, Service.Status)
    assertEquals(result.nodeIdentity.instanceId, instance)
    assertEquals(result.nodeIdentity.host, "192.168.1.100")
    assertEquals(result.nodeIdentity.port, port)
    assertEquals(new String(result.payload, "UTF-8"), jsonPayload)

  test("UDPHeader.parse does NOT return None for local instance anymore (filtering is done by transport)"):
    val instance = "local-instance"
    val pi = PortAndInstance(8080, instance)
    val headerData = (s"FDSWARM|Status|$pi|${BuildInfo.dataVersion}|\n").getBytes("UTF-8")
    val address = InetAddress.getLoopbackAddress
    val packet = new DatagramPacket(headerData, headerData.length, address, 1234)
    val result = UDPHeader.parse(packet)
    assert(result.isDefined)
    assertEquals(result.get.nodeIdentity.instanceId, instance)

  test("UDPHeader.parse correctly parses gzipped payload"):
    val instance = "test-instance"
    val port = 8082
    val pi = PortAndInstance(port, instance)
    val jsonPayload = "\"gzipped-payload\""
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos)
    gzos.write(jsonPayload.getBytes("UTF-8"))
    gzos.close()
    val gzippedPayload = baos.toByteArray
    
    val headerPart = s"FDSWARM|Status|$pi|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val packetData = new Array[Byte](headerPart.length + gzippedPayload.length)
    System.arraycopy(headerPart, 0, packetData, 0, headerPart.length)
    System.arraycopy(gzippedPayload, 0, packetData, headerPart.length, gzippedPayload.length)
    
    val address = InetAddress.getByName("192.168.1.101")
    val packet = new DatagramPacket(packetData, packetData.length, address, 1234)

    val result = UDPHeader.parse(packet).get
    assertEquals(result.service, Service.Status)
    assertEquals(result.nodeIdentity.instanceId, instance)
    assert(result.payload.sameElements(gzippedPayload))

  test("UDPHeader.parse fails on invalid prefix"):
    val headerData = s"INVALID|Status|8080-instance|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
    val packet = new DatagramPacket(headerData, headerData.length, InetAddress.getLoopbackAddress, 1234)
    intercept[IllegalArgumentException](UDPHeader.parse(packet))

  test("UDPHeader.parse fails on invalid version"):
    val headerData = s"FDSWARM|Status|8080-instance|999|\n".getBytes("UTF-8")
    val packet = new DatagramPacket(headerData, headerData.length, InetAddress.getLoopbackAddress, 1234)
    intercept[IllegalArgumentException](UDPHeader.parse(packet))

  test("UDPHeader.parse handles InstanceQuery and InstanceResponse"):
    val pi = PortAndInstance(8080, "other-instance")
    val queryPayload = "target-instance".getBytes("UTF-8")
    val headerData = (s"FDSWARM|InstanceQuery|$pi|${BuildInfo.dataVersion}|\n" + "target-instance").getBytes("UTF-8")
    val packet = new DatagramPacket(headerData, headerData.length, InetAddress.getByName("1.2.3.4"), 1234)
    
    val result = UDPHeader.parse(packet).get
    assertEquals(result.service, Service.InstanceQuery)
    assertEquals(new String(result.payload, "UTF-8"), "target-instance")

    val responseData = (s"FDSWARM|InstanceResponse|$pi|${BuildInfo.dataVersion}|\n" + "1.2.3.4:8080-target-instance").getBytes("UTF-8")
    val responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getByName("1.2.3.4"), 1234)
    val responseResult = UDPHeader.parse(responsePacket).get
    assertEquals(responseResult.service, Service.InstanceResponse)
    assertEquals(new String(responseResult.payload, "UTF-8"), "1.2.3.4:8080-target-instance")
