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

package fdswarm.replication.status

import fdswarm.{MockStartupInfo, StationConfigManager, TestDirectory}
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.bands.{BandCatalog, BandModeBuilder, ModeCatalog}
import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.fx.qso.FdHour
import fdswarm.fx.station.{StationEditor, StationStore}
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.{LocalNodeStatus, NodeStatus, StatusMessage}
import fdswarm.util.{AgeStyleService, MockNodeIdentityManager}
import munit.FunSuite

import java.time.Instant

class SwarmDataTest extends FunSuite:
  test("resolveFields with FdHours expands known and newly discovered hours"):
    val testDir = new TestDirectory
    val stationManager = new StationConfigManager(
      testDir,
      MockStartupInfo
    )
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin
    )
    val bandCatalog = new BandCatalog(config)
    val modeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new BandModeBuilder(
      bandCatalog,
      modeCatalog
    )
    val selectedBandModeStore = new SelectedBandModeManager(
      testDir,
      bandModeBuilder,
      MockStartupInfo
    )
    val localNodeStatus = new LocalNodeStatus(
      MockNodeIdentityManager(),
      stationManager,
      selectedBandModeStore,
      () => null
    )
    val stationStore = new StationStore(
      stationManager
    )
    val stationEditor = new StationEditor(
      stationStore
    )
    val swarmData = new SwarmData(
      MockNodeIdentityManager(),
      localNodeStatus,
      stationEditor,
      new AgeCellStyleRefresher(
        config = config,
        ageStyleService = new AgeStyleService(
          config = config
        )
      )
    )
    val hour1 = FdHour(10, 1)
    val hour2 = FdHour(10, 2)

    val fields = Seq(NodeDataField.HostName, FdHours)

    swarmData.addKnownFdHours(Seq(hour1))
    val resolvedAfterFirstUpdate = swarmData.resolveFields(fields)
    assert(
      resolvedAfterFirstUpdate.contains(NodeDataField.HostName),
      "expected HostName"
    )
    assert(
      resolvedAfterFirstUpdate.contains(NodeDataField.FdHoursField(FdHours(hour1))),
      "expected first FdHour field"
    )
    assert(
      !resolvedAfterFirstUpdate.contains(NodeDataField.FdHoursField(FdHours(hour2))),
      "did not expect second FdHour field yet"
    )

    swarmData.addKnownFdHours(Seq(hour1, hour2))
    val resolvedAfterSecondUpdate = swarmData.resolveFields(fields)
    assert(
      resolvedAfterSecondUpdate.contains(NodeDataField.HostName),
      "expected HostName after second update"
    )
    assert(
      resolvedAfterSecondUpdate.contains(NodeDataField.FdHoursField(FdHours(hour1))),
      "expected first FdHour after second update"
    )
    assert(
      resolvedAfterSecondUpdate.contains(NodeDataField.FdHoursField(FdHours(hour2))),
      "expected second FdHour after second update"
    )

    testDir.cleanup()

  test("stale remote node is auto-purged quickly"):
    val testDir = new TestDirectory
    val stationManager = new StationConfigManager(
      testDir,
      MockStartupInfo
    )
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  ageRefreshDuration = 1h
        |  ageStyles {
        |    nodeAging {
        |      purgeAfter = 50ms
        |      thresholds = [
        |        { duration = 10ms, style = "fresh" }
        |      ]
        |      olderStyle = "stale"
        |    }
        |  }
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin
    )
    val bandCatalog = new BandCatalog(
      config
    )
    val modeCatalog = new ModeCatalog(
      config
    )
    val bandModeBuilder = new BandModeBuilder(
      bandCatalog,
      modeCatalog
    )
    val selectedBandModeStore = new SelectedBandModeManager(
      testDir,
      bandModeBuilder,
      MockStartupInfo
    )
    val localNodeStatus = new LocalNodeStatus(
      MockNodeIdentityManager(),
      stationManager,
      selectedBandModeStore,
      () => null
    )
    val stationStore = new StationStore(
      stationManager
    )
    val stationEditor = new StationEditor(
      stationStore
    )
    val ageCellStyleRefresher = new AgeCellStyleRefresher(
      config = config,
      ageStyleService = new AgeStyleService(
        config = config
      )
    )
    val swarmData = new SwarmData(
      MockNodeIdentityManager(),
      localNodeStatus,
      stationEditor,
      ageCellStyleRefresher
    )

    val remoteNode = fdswarm.util.NodeIdentity(
      "192.168.1.44",
      8080,
      "remote-host",
      "remote-instance"
    )
    val bno = BandModeOperator(
      Callsign("WA9NNN"),
      BandMode(
        "40M",
        "CW"
      ),
      Instant.now()
    )
    val contestConfig = ContestConfig(
      ContestType.ARRL,
      Callsign("WA9NNN"),
      1,
      "A",
      "IL"
    )
    val staleStatus = NodeStatus(
      statusMessage = StatusMessage(hash = Nil,, bandNodeOperator = bno, contestConfig = contestConfig),
      nodeIdentity = remoteNode,
      received = Instant.now().minusSeconds(5),
      isLocal = false
    )
    swarmData.update(
      staleStatus
    )
    assert(
      swarmData.nodeMap.contains(
        remoteNode
      ),
      "expected remote node to exist immediately after update"
    )
    ageCellStyleRefresher.runPurgeCycle(
      now = Instant.now()
    )

    assert(
      !swarmData.nodeMap.contains(
        remoteNode
      ),
      "expected stale remote node to be purged immediately"
    )
    testDir.cleanup()
