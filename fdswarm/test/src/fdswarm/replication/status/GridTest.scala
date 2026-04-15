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

import fdswarm.fx.GridBuilder
import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.{HashCount, NodeStatus, StatusMessage}
import fdswarm.util.NodeIdentity
import munit.FunSuite

import java.time.Instant

import scala.jdk.CollectionConverters.*

class GridTest extends FunSuite:
  private val dummyBno = BandModeOperator(
    Callsign("WA9NNN"),
    BandMode("40M", "CW"),
    Instant.parse("2026-03-16T20:11:04Z")
  )

  private val dummyContestConfig = ContestConfig(
    ContestType.ARRL,
    Callsign("WA9NNN"),
    1,
    "A",
    "IL",
    Instant.parse("2026-03-16T20:11:04Z")
  )

  private def initJavaFx(): Unit =
    try
      new javafx.embed.swing.JFXPanel()
    catch
      case _: Throwable =>

  private def newAgeStyleService() =
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm.ageStyles {
        |  nodeAging {
        |    thresholds = [
        |      { duration = 12.0, style = "fresh" }
        |      { duration = 20.0, style = "stale" }
        |    ]
        |  }
        |}
      """.stripMargin
    )
    new fdswarm.util.AgeStyleService(config)

  private def gridContainsText(
    gridPane: javafx.scene.layout.GridPane,
    text: String
  ): Boolean =
    gridPane.getChildren.asScala.exists {
      case l: javafx.scene.control.Label if l.getText == text => true
      case h: javafx.scene.layout.HBox =>
        h.getChildren.asScala.exists {
          case l: javafx.scene.control.Label if l.getText == text => true
          case _ => false
        }
      case _ => false
    }

  test("Grid.populate should include all current SwarmStatusGrid rows".ignore):
    initJavaFx()
    val nowProperty = scalafx.beans.property.LongProperty(
      Instant.parse("2026-03-16T20:12:00Z").toEpochMilli
    )
    val ageStyleService = newAgeStyleService()

    val node1 = NodeStatus(
      StatusMessage(
        hashCount = HashCount(qsoCount = 5),
        bandNodeOperator = dummyBno,
        contestConfig = dummyContestConfig
      ),
      NodeIdentity("192.168.1.1", 8080, "111", "node1"),
      Instant.parse("2026-03-16T20:11:30Z"),
      isLocal = false
    )
    val node2 = NodeStatus(
      StatusMessage(
        hashCount = HashCount(qsoCount = 3),
        bandNodeOperator = dummyBno,
        contestConfig = dummyContestConfig
      ),
      NodeIdentity("192.168.1.2", 8080, "222", "node2"),
      Instant.parse("2026-03-16T20:11:30Z"),
      isLocal = false
    )

    val builder = new GridBuilder()
    val grid = SwarmStatusGrid(
      Seq(node1, node2),
      nowProperty,
      ageStyleService,
      "some-id",
      _ => ()
    )

    grid.populate(
      builder,
      _ => "test-style"
    )

    val gridPane = builder.result

    assert(gridContainsText(gridPane, "Instance Id"))
    assert(gridContainsText(gridPane, "Host"))
    assert(gridContainsText(gridPane, "Contest"))
    assert(gridContainsText(gridPane, "Age"))
    assert(gridContainsText(gridPane, "Qso Count"))
    assert(gridContainsText(gridPane, "Operator"))
    assert(gridContainsText(gridPane, "Band/Mode"))
    assert(gridContainsText(gridPane, "node1"))
    assert(gridContainsText(gridPane, "node2"))
    assert(gridContainsText(gridPane, "192.168.1.1"))
    assert(gridContainsText(gridPane, "192.168.1.2"))
    assert(gridContainsText(gridPane, dummyContestConfig.display))
    assert(gridContainsText(gridPane, "5"))
    assert(gridContainsText(gridPane, "3"))
    assert(gridPane.getChildren.asScala.nonEmpty)

  test("Grid.populate should show remove control and our-node age label".ignore):
    initJavaFx()
    val nowProperty = scalafx.beans.property.LongProperty(
      Instant.parse("2026-03-16T20:12:00Z").toEpochMilli
    )
    val ageStyleService = newAgeStyleService()

    var removedNode: Option[NodeIdentity] = None
    val remoteNodeIdentity = NodeIdentity("192.168.1.10", 8080, "111", "remote-node")
    val ourNodeIdentity = NodeIdentity("127.0.0.1", 8080, "222", "our-node")

    val remoteNode = NodeStatus(
      StatusMessage(
        hashCount = HashCount(),
        bandNodeOperator = dummyBno,
        contestConfig = dummyContestConfig
      ),
      remoteNodeIdentity,
      Instant.parse("2026-03-16T20:11:30Z"),
      isLocal = false
    )
    val ourNode = NodeStatus(
      StatusMessage(
        hashCount = HashCount(),
        bandNodeOperator = dummyBno,
        contestConfig = dummyContestConfig
      ),
      ourNodeIdentity,
      Instant.parse("2026-03-16T20:11:30Z"),
      isLocal = true
    )

    val builder = new GridBuilder()
    val grid = SwarmStatusGrid(
      Seq(remoteNode, ourNode),
      nowProperty,
      ageStyleService,
      "our-node",
      nodeIdentity => removedNode = Some(nodeIdentity)
    )

    grid.populate(
      builder,
      _ => "test-style"
    )

    val gridPane = builder.result
    val deleteButtons = gridPane.getChildren.asScala.collect {
      case h: javafx.scene.layout.HBox =>
        h.getChildren.asScala.collect { case b: javafx.scene.control.Button => b }
    }.flatten

    assertEquals(
      deleteButtons.size,
      1
    )
    deleteButtons.head.fire()
    assertEquals(
      removedNode,
      Some(remoteNodeIdentity)
    )

    val ourNodeLabel = gridPane.getChildren.asScala.collectFirst {
      case l: javafx.scene.control.Label if l.getText == "Our Node" => l
    }
    assert(ourNodeLabel.isDefined)
    assert(ourNodeLabel.get.getStyleClass.contains("ourNode"))
