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

import fdswarm.{MockStartupInfo, StationConfigManager, TestDirectory}
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.bands.{BandCatalog, BandModeBuilder, ModeCatalog}
import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.fx.qso.FdHour
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.status.SwarmStatus
import fdswarm.store.FdHourDigest
import fdswarm.util.{MockNodeIdentityManager, NodeIdentity}
import jakarta.inject.Provider
import munit.FunSuite

import java.time.Instant

class SwarmStatusTest extends FunSuite:
  private val dummyBno = BandModeOperator(Callsign("WA9NNN"), BandMode("40M", "CW"), Instant.parse("2026-03-16T20:11:04Z"))
  private val dummyContestConfig = ContestConfig(ContestType.ARRL, Callsign("WA9NNN"), 1, "A", "IL")

  test("SwarmStatus.put should update nodeMap"):
    val testDir = new TestDirectory
    val stationManager = new StationConfigManager(testDir, MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new BandCatalog(config)
    val modeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new BandModeBuilder(bandCatalog, modeCatalog)
    val selectedBandModeStore = new SelectedBandModeManager(testDir, bandModeBuilder, MockStartupInfo)
    val swarmStatusPaneProvider: Provider[status.SwarmStatusPane] = () => null
    val contestConfigManagerProvider: Provider[fdswarm.fx.contest.ContestConfigManager] = () => null
    val swarmStatus = new SwarmStatus(testDir, MockNodeIdentityManager(), stationManager, selectedBandModeStore, swarmStatusPaneProvider, contestConfigManagerProvider)
    val hp = NodeIdentity("192.168.1.100", 8080, "test-instance", "xxx")
    val hour = FdHour(15, 12)
    val digest = FdHourDigest(hour, 10, "abc")
    val statusMessage = StatusMessage(Seq(digest), dummyBno, contestConfig = dummyContestConfig)
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    swarmStatus.put(nodeStuff)

    assert(swarmStatus.nodeMap.contains(hp), "nodeMap should contain node identity")
    val receivedStatus = swarmStatus.nodeMap(hp)
    assertEquals(receivedStatus.qsoCount, 10, "qsoCount should be 10")

    testDir.cleanup()

  test("SwarmStatus.clear should retain local node data"):
    val testDir = new TestDirectory
    val stationManager = new StationConfigManager(testDir, MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new BandCatalog(config)
    val modeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new BandModeBuilder(bandCatalog, modeCatalog)
    val selectedBandModeStore = new SelectedBandModeManager(testDir, bandModeBuilder, MockStartupInfo)
    val localNi = NodeIdentity("127.0.0.1", 8080, "local-instance", "xxx")
    val remoteNi = NodeIdentity("192.168.1.100", 8080, "remote-instance", "yyy")
    val mockNodeIdentityManager = new MockNodeIdentityManager(localNi)
    val swarmStatusPaneProvider: Provider[status.SwarmStatusPane] = () => null
    val contestConfigManagerProvider: Provider[fdswarm.fx.contest.ContestConfigManager] = () => null
    val swarmStatus = new SwarmStatus(testDir, mockNodeIdentityManager, stationManager, selectedBandModeStore, swarmStatusPaneProvider, contestConfigManagerProvider)

    val hour = FdHour(15, 12)
    val digest = FdHourDigest(hour, 10, "abc")
    val statusMessage = StatusMessage(Seq(digest), dummyBno, contestConfig = dummyContestConfig)

    // Put local node data
    swarmStatus.put(ReceivedNodeStatus(statusMessage, localNi))
    // Put remote node data
    swarmStatus.put(ReceivedNodeStatus(statusMessage, remoteNi))

    assertEquals(swarmStatus.nodeMap.size, 2)

    swarmStatus.clear()
    assertEquals(swarmStatus.nodeMap.size, 1, "nodeMap should have 1 node after clear")

    // Test remove
    swarmStatus.put(ReceivedNodeStatus(statusMessage, remoteNi))
    assertEquals(swarmStatus.nodeMap.size, 2)
    swarmStatus.remove(remoteNi)
    assertEquals(swarmStatus.nodeMap.size, 1)
    assert(!swarmStatus.nodeMap.contains(remoteNi))

    // Test remove local node (should do nothing)
    swarmStatus.remove(localNi)
    assertEquals(swarmStatus.nodeMap.size, 1)
    assert(swarmStatus.nodeMap.contains(localNi), "nodeMap should still contain local node")
    assert(!swarmStatus.nodeMap.contains(remoteNi), "nodeMap should NOT contain remote node")

    testDir.cleanup()

  test("SwarmStatus should persist state"):
    val testDir = new TestDirectory
    val stationManager = new StationConfigManager(testDir, MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new BandCatalog(config)
    val modeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new BandModeBuilder(bandCatalog, modeCatalog)
    val selectedBandModeStore = new SelectedBandModeManager(testDir, bandModeBuilder, MockStartupInfo)
    val hp = NodeIdentity("192.168.1.101", 9090, "test-instance-2", "xxx")
    val hour = FdHour(16, 13)
    val digest = FdHourDigest(hour, 5, "def")
    val statusMessage = StatusMessage(Seq(digest), dummyBno, contestConfig = dummyContestConfig)
    val nodeStuff = ReceivedNodeStatus(statusMessage, hp)

    // 1. Create SwarmStatus, put data, and it should save
    val swarmStatusPaneProvider: Provider[status.SwarmStatusPane] = () => null
    val contestConfigManagerProvider: Provider[fdswarm.fx.contest.ContestConfigManager] = () => null
    val swarmStatus1 = new SwarmStatus(testDir, MockNodeIdentityManager(), stationManager, selectedBandModeStore, swarmStatusPaneProvider, contestConfigManagerProvider, statusPersist = true)
    swarmStatus1.put(nodeStuff)
    
    // 2. verify file exists
    assert(os.exists(testDir() / "swarmStatus.json"))

    testDir.cleanup()
