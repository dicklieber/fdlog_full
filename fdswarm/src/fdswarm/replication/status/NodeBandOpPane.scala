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
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.TitledPane

@Singleton
class NodeBandOpPane @Inject()(swarmStatus: SwarmStatus):

  val node: TitledPane = new TitledPane {
    text = "Swarm"
    collapsible = false
    content = buildGrid()
  }

  def refresh(): Unit =
    node.content = buildGrid()

  private def buildGrid() =
    val builder = GridBuilder()
    val nodes = swarmStatus.nodeMap.toSeq.sortBy(_._1)

    builder("", nodes.map(_._1)*)
    builder("operator", nodes.map(_._2.statusMessage.bandNodeOperator.operator.toString)*)
    builder("bandMode", nodes.map(_._2.statusMessage.bandNodeOperator.bandMode.toString)*)
    builder("hostName", nodes.map(_._1.hostName)*)

    builder.result
