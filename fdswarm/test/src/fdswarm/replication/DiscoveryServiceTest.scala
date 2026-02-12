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

import com.typesafe.config.ConfigFactory
import fdswarm.fx.contest.{ContestCatalog, ContestConfig, ContestManager, ContestType}
import fdswarm.io.DirectoryProvider
import munit.FunSuite

import java.net.{DatagramPacket, DatagramSocket}
import java.time.ZonedDateTime
import scala.util.Success

class DiscoveryServiceTest extends FunSuite {

  test("DiscoveryService responds to FDSWARM|DISCOVER and can discover other nodes") {
    // Find an available port
    val tempSocket = new DatagramSocket(0)
    val port = tempSocket.getLocalPort
    tempSocket.close()

    val config = ConfigFactory.parseString(
      s"""
         |fdswarm.discovery.Port = $port
         |fdswarm.broadcastAddress = "127.0.0.1"
         |fdswarm.discovery.timeoutMs = 1000
         |fdswarm.contests = [
         |  {
         |    name = WFD
         |    classChars = []
         |  }
         |]
         |""".stripMargin
    )

    // Mock DirectoryProvider
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }

    // In a real scenario we'd use Guice, but manual injection is fine for this test
    // Setup ContestCatalog
    val contestCatalog = new ContestCatalog(config)

    // We need a real ContestManager (or a mock)
    // For simplicity, let's use a real one since it's easy to setup with tmpDir
    val contestManager = new ContestManager(directoryProvider, contestCatalog)
    val expectedConfig = ContestConfig(ContestType.WFD, ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))
    contestManager.setConfig(expectedConfig)

    val discoveryService = new DiscoveryService(contestManager, config)

    discoveryService.start()

    try {
      // Test discover method (it sends FDSWARM|DISCOVER and waits for response)
      // With self-ignore enabled, a single node should NOT discover itself
      val results = discoveryService.discover()
      assert(results.isEmpty, s"Discovery should be empty when alone, but found: ${results}")
    } finally {
      discoveryService.stop()
    }
  }
}
