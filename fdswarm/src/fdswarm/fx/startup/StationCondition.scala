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

package fdswarm.fx.startup

import jakarta.inject.{Inject, Singleton}
import fdswarm.fx.contest.DiscoveryWire
import fdswarm.fx.station.{StationEditor, StationStore}
import fdswarm.util.NodeIdentity
import scalafx.stage.Window

@Singleton
class StationCondition @Inject()(
    private val stationStore: StationStore,
    private val stationEditor: StationEditor
) extends StartupCondition:
  override def name: String = "Station"

  override def editButton(ownerWindow: Window): Unit =
    stationEditor.show(ownerWindow)

  override def update(discovered: Map[NodeIdentity, DiscoveryWire]): Unit =
    val station = stationStore.station.value
    val ourOperator = station.operator
    val duplicateOperator = discovered.values.exists(_.stationConfig.operator == ourOperator)

    val newProblems = scala.collection.mutable.ListBuffer[String]()
    if duplicateOperator then
      newProblems += s"Operator $ourOperator is already in use on another node!"
    if !fdswarm.model.Callsign.isValid(station.operator.value) then
      newProblems += s"Invalid Operator callsign: ${station.operator.value}"
    
    problems.clear()
    problems ++= newProblems
    details.value = s"Operator: ${station.operator.value}, Rig: ${station.rig}, Antenna: ${station.antenna}"
