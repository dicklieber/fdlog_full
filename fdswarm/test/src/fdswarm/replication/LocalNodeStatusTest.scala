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

import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.fx.qso.FdHour
import fdswarm.fx.station.StationConfig
import fdswarm.model.Callsign
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import munit.FunSuite

class LocalNodeStatusTest extends FunSuite:
  private val dummyContestConfig = ContestConfig(ContestType.ARRL, Callsign("WA9NNN"), 1, "A", "IL")

  test("LocalNodeStatus initializes immediately when contest config already exists"):
    val testDir = new fdswarm.TestDirectory
    val stationManager = new fdswarm.StationConfigManager(testDir, fdswarm.MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] },
        |    { bandName = "40m", startFrequencyHz = 7000000, endFrequencyHz = 7300000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new fdswarm.fx.bands.BandCatalog(config)
    val modeCatalog = new fdswarm.fx.bands.ModeCatalog(config)
    val bandModeBuilder = new fdswarm.fx.bands.BandModeBuilder(bandCatalog, modeCatalog)
    val selectedBandModeStore = new fdswarm.fx.bandmodes.SelectedBandModeManager(testDir, bandModeBuilder, fdswarm.MockStartupInfo)
    val contestManager = new fdswarm.fx.contest.ContestConfigManager(testDir, () => null, new fdswarm.util.FilenameStamp(), 1)
    contestManager.setConfig(dummyContestConfig)
    val nodeIdentityManager = new fdswarm.util.MockNodeIdentityManager(NodeIdentity("127.0.0.1", 8080, "local-instance", "x"))
    val localNodeStatus = new LocalNodeStatus(nodeIdentityManager, stationManager, selectedBandModeStore, () => contestManager)

    assertEquals(localNodeStatus.updates.size(), 1)
    assertEquals(localNodeStatus.updates.get(0).isLocal, true)
    assertEquals(localNodeStatus.updates.get(0).statusMessage.fdDigests, Seq.empty)
    assertEquals(localNodeStatus.updates.get(0).statusMessage.contestConfig, dummyContestConfig)

    testDir.cleanup()

  test("LocalNodeStatus updates on digests, station config, and band mode with isLocal=true"):
    val testDir = new fdswarm.TestDirectory
    val stationManager = new fdswarm.StationConfigManager(testDir, fdswarm.MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] },
        |    { bandName = "40m", startFrequencyHz = 7000000, endFrequencyHz = 7300000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new fdswarm.fx.bands.BandCatalog(config)
    val modeCatalog = new fdswarm.fx.bands.ModeCatalog(config)
    val bandModeBuilder = new fdswarm.fx.bands.BandModeBuilder(bandCatalog, modeCatalog)
    val selectedBandModeStore = new fdswarm.fx.bandmodes.SelectedBandModeManager(testDir, bandModeBuilder, fdswarm.MockStartupInfo)
    val contestManager = new fdswarm.fx.contest.ContestConfigManager(testDir, () => null, new fdswarm.util.FilenameStamp(), 1)
    val nodeIdentityManager = new fdswarm.util.MockNodeIdentityManager(NodeIdentity("127.0.0.1", 8080, "local-instance", "x"))
    val localNodeStatus = new LocalNodeStatus(nodeIdentityManager, stationManager, selectedBandModeStore, () => contestManager)

    val digest = FdHourDigest(FdHour(15, 12), 10, "abc")
    localNodeStatus.updateDigests(Seq(digest))
    assertEquals(localNodeStatus.updates.size(), 0, "No status should be emitted before contest config exists")

    contestManager.setConfig(dummyContestConfig)
    assertEquals(localNodeStatus.updates.size(), 1)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).isLocal, true)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).statusMessage.fdDigests, Seq(digest))

    val t1 = localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).received
    Thread.sleep(2)
    stationManager.setStation(StationConfig(operator = Callsign("K1ABC"), rig = "Rig", antenna = "Wire"))
    assertEquals(localNodeStatus.updates.size(), 2)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).isLocal, true)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).statusMessage.bandNodeOperator.operator, Callsign("K1ABC"))
    assert(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).received.isAfter(t1), "received should advance for each new local status")

    val t2 = localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).received
    Thread.sleep(2)
    selectedBandModeStore.save(bandModeBuilder("40m", "CW"))
    assertEquals(localNodeStatus.updates.size(), 3)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).isLocal, true)
    assertEquals(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).statusMessage.bandNodeOperator.bandMode.toString, "40m CW")
    assert(localNodeStatus.updates.get(localNodeStatus.updates.size() - 1).received.isAfter(t2), "received should advance for each new local status")

    testDir.cleanup()
