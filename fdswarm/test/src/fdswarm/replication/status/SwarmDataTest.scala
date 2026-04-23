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

import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.{HashCount, NodeStatus, StatusMessage}
import fdswarm.util.NodeIdentity
import munit.FunSuite

class SwarmDataTest extends FunSuite:
  test("static status fields include qsoCount, hash, and exchange columns"):
    assert(
      NodeDataField.staticFields.contains(
        NodeDataField.QsoCount
      )
    )
    assert(
      NodeDataField.staticFields.contains(
        NodeDataField.Hash
      )
    )
    assert(
      NodeDataField.staticFields.contains(
        NodeDataField.Exchange
      )
    )

  test("contest config styles are empty when all nodes match"):
    val styles = SwarmData.contestConfigFieldStyles(
      statuses = Seq(
        nodeStatus(
          hostName = "alpha",
          contestConfig = contestConfig(
            contestType = ContestType.WFD,
            callsign = "W9AAA",
            transmitters = 1,
            stationClass = "A",
            section = "IL"
          )
        ),
        nodeStatus(
          hostName = "beta",
          contestConfig = contestConfig(
            contestType = ContestType.WFD,
            callsign = "W9AAA",
            transmitters = 1,
            stationClass = "A",
            section = "IL"
          )
        )
      )
    )
    assertEquals(
      obtained = styles,
      expected = Map.empty[(NodeIdentity, NodeDataField), String]
    )

  test("contest config styles mark majority green and minority as variants"):
    val styles = SwarmData.contestConfigFieldStyles(
      statuses = Seq(
        nodeStatus(
          hostName = "alpha",
          contestConfig = contestConfig(
            contestType = ContestType.WFD,
            callsign = "W9AAA",
            transmitters = 1,
            stationClass = "A",
            section = "IL"
          )
        ),
        nodeStatus(
          hostName = "beta",
          contestConfig = contestConfig(
            contestType = ContestType.ARRL,
            callsign = "W9BBB",
            transmitters = 2,
            stationClass = "A",
            section = "IL"
          )
        ),
        nodeStatus(
          hostName = "gamma",
          contestConfig = contestConfig(
            contestType = ContestType.WFD,
            callsign = "W9AAA",
            transmitters = 1,
            stationClass = "A",
            section = "IL"
          )
        )
      )
    )
    assert(
      styles(
        (
          NodeIdentity(
            hostIp = "10.0.0.1",
            port = 8090,
            hostName = "alpha",
            instanceId = "alpha-id"
          ),
          NodeDataField.Exchange
        )
      )
      == "contestConfigMajority"
    )
    assert(
      styles(
        (
          NodeIdentity(
            hostIp = "10.0.0.1",
            port = 8090,
            hostName = "beta",
            instanceId = "beta-id"
          ),
          NodeDataField.Exchange
        )
      )
      != "contestConfigMajority"
    )
    assert(
      styles(
        (
          NodeIdentity(
            hostIp = "10.0.0.1",
            port = 8090,
            hostName = "beta",
            instanceId = "beta-id"
          ),
          NodeDataField.ContestType
        )
      )
      != "contestConfigMajority"
    )

  private def contestConfig(
    contestType: ContestType,
    callsign: String,
    transmitters: Int,
    stationClass: String,
    section: String
  ): ContestConfig =
    ContestConfig(
      contestType = contestType,
      ourCallsign = Callsign(
        callsign
      ),
      transmitters = transmitters,
      ourClass = stationClass,
      ourSection = section
    )

  private def nodeStatus(
    hostName: String,
    contestConfig: ContestConfig
  ): NodeStatus =
    NodeStatus(
      statusMessage = StatusMessage(hashCount = HashCount(hash = "", qsoCount = 0),
        bandNodeOperator = BandModeOperator(operator = Callsign("N0CALL"), bandMode = BandMode("20M SSB")),
        contestConfig = contestConfig,),
      nodeIdentity = NodeIdentity(
        hostIp = "10.0.0.1",
        port = 8090,
        hostName = hostName,
        instanceId = s"$hostName-id"
      ),
      isLocal = false
    )
