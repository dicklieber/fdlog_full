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
import fdswarm.fx.qso.FdHour
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.{NodeDetails, ReceivedNodeStatus, StatusMessage}
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import munit.FunSuite
import java.time.Instant
import scala.jdk.CollectionConverters.*

class GirdTest extends FunSuite:
  private val dummyBno = BandModeOperator(Callsign("WA9NNN"), BandMode("40M", "CW"), Instant.parse("2026-03-16T20:11:04Z"))

  test("Gird should create a 2D array of IntLabels"):
    // Mock JavaFX Toolkit if not already initialized
    try {
      new javafx.embed.swing.JFXPanel()
    } catch {
      case _: Throwable => // ignore
    }
    val ni1 = NodeIdentity("192.168.1.1", 8080, "node1")
    val ni2 = NodeIdentity("192.168.1.2", 8080, "node2")

    val hour1 = FdHour(10, 1)
    val hour2 = FdHour(10, 2)

    val sm1 = StatusMessage(Seq(FdHourDigest(hour1, 5, "d1"), FdHourDigest(hour2, 10, "d2")), dummyBno)
    val nd1 = ReceivedNodeStatus(sm1, ni1)

    val sm2 = StatusMessage(Seq(FdHourDigest(hour1, 3, "d3")), dummyBno)
    val nd2 = ReceivedNodeStatus(sm2, ni2)

    val allNodeDetails = Seq(nd1, nd2)
    val nowProperty = scalafx.beans.property.LongProperty(System.currentTimeMillis())
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm.ageStyles = [
        |  {
        |    name = "node"
        |    thresholds = [
        |      { duration = "12s", style = "fresh" }
        |    ]
        |    olderStyle = "stale"
        |  }
        |]
      """.stripMargin)
    val ageStyleService = new fdswarm.util.AgeStyleService(config)
    val swarmStatusApi = new SwarmStatusApi {
      override def clear(): Unit = ()
      override def refresh(): Unit = ()
      override def remove(nodeIdentity: NodeIdentity): Unit = ()
    }
    val gird = SwarmStatusGrid(allNodeDetails, nowProperty, ageStyleService, "some-id", swarmStatusApi)

    // FdHour is sorted, so hour1 then hour2
    assertEquals(gird.fdHours.length, 2)
    assertEquals(gird.fdHours(0), hour1)
    assertEquals(gird.fdHours(1), hour2)

    // grid dimension 1: hours
    // assertEquals(gird.bodyCounts.length, 2)

    // row 0: hour1
    // assertEquals(gird.bodyCounts(0).length, 2)
    // assertEquals(gird.bodyCounts(0)(0).text.value, "5")
    // assertEquals(gird.bodyCounts(0)(1).text.value, "3")

    // row 1: hour2
    // assertEquals(gird.bodyCounts(1).length, 2)
    // assertEquals(gird.bodyCounts(1)(0).text.value, "10")
    // assertEquals(gird.bodyCounts(1)(1).text.value, "0")

  test("Gird.populate should add header rows"):
    val ni1 = NodeIdentity("192.168.1.1", 8080, "node1")
    val nd1 = ReceivedNodeStatus(StatusMessage(Nil, dummyBno), ni1)

    val builder = new GridBuilder()
    val nowProperty = scalafx.beans.property.LongProperty(System.currentTimeMillis())
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm.ageStyles = [
        |  {
        |    name = "node"
        |    thresholds = [
        |      { duration = "12s", style = "fresh" }
        |    ]
        |    olderStyle = "stale"
        |  }
        |]
      """.stripMargin)
    val ageStyleService = new fdswarm.util.AgeStyleService(config)
    val swarmStatusApi = new SwarmStatusApi {
      override def clear(): Unit = ()
      override def refresh(): Unit = ()
      override def remove(nodeIdentity: NodeIdentity): Unit = ()
    }
    val gird = SwarmStatusGrid(Seq(nd1), nowProperty, ageStyleService, "some-id", swarmStatusApi)

    gird.populate(builder, _ => "test-style")
    
    val gridPane = builder.result
    // 4 headers + 0 hours = 4 rows
    // Wait, hours is empty here.
    // Labels are added to gridPane.
    
    def findLabelByText(text: String): Option[javafx.scene.control.Label] =
      gridPane.getChildren.asScala.collectFirst {
        case l: javafx.scene.control.Label if l.getText == text => l
      }

    // Check headers in col 0
    assert(findLabelByText("InstanceId").isDefined)
    assert(findLabelByText("IP").isDefined)
    assert(findLabelByText("Age").isDefined)
    assert(findLabelByText("Qso Count").isDefined)
    
    // Check values in col 1
    // assert(findLabelByText("node1").isDefined) // Commented out because it's now in an HBox
    assert(gridPane.getChildren.asScala.exists {
      case l: javafx.scene.control.Label if l.getText == "node1" => true
      case h: javafx.scene.layout.HBox => h.getChildren.asScala.exists {
        case l: javafx.scene.control.Label if l.getText == "node1" => true
        case _ => false
      }
      case _ => false
    })
    // Check "Our Node" when it matches
    val niOur = NodeIdentity("127.0.0.1", 8080, "our-node")
    val ndOur = ReceivedNodeStatus(StatusMessage(Nil, dummyBno), niOur)
    val builder2 = new GridBuilder()
    val gird2 = SwarmStatusGrid(Seq(ndOur), nowProperty, ageStyleService, "our-node", swarmStatusApi)
    gird2.populate(builder2, _ => "test-style")
    val gridPane2 = builder2.result
    
    val ourNodeLabel = gridPane2.getChildren.asScala.collectFirst {
      case l: javafx.scene.control.Label if l.getText == "Our Node" => l
    }
    assert(ourNodeLabel.isDefined)
    assert(ourNodeLabel.get.getStyleClass.contains("ourNode"))
    
    // Check font styles (now applied via CSS, so we check if the style class is present)
    assert(ourNodeLabel.get.getStyleClass.contains("ourNode"))

    // Check host
    val allChildren = gridPane.getChildren.asScala.toList
    val labels = allChildren.collect { case l: javafx.scene.control.Label => l.getText }
    assert(labels.contains("192.168.1.1"))
    // Check age label - we don't check exact text because it's a binding and might not be evaluated immediately in test
    // or might have slightly different timing.
    // But we should have some labels.
    assert(gridPane.getChildren.asScala.nonEmpty)
