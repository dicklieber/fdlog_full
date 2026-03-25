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
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.fx.discovery.{ContestDiscovery, DiscoveryWire}
import fdswarm.util.NodeIdentity
import scalafx.stage.Window

@Singleton
class ContestCondition @Inject()(
                                  private val contestManager: ContestConfigManager,
                                  private val contestDiscovery: ContestDiscovery
) extends StartupCondition:
  override def name: String = "Contest"

  override def editButton(ownerWindow: Window): Unit =
//    contestManager.show(ownerWindow)
    throw new NotImplementedError("") //todo

  override def update(discovered: Map[NodeIdentity, DiscoveryWire]): Unit =
    val config = contestManager.config
    val currentDetails =
      s"Callsign: ${config.ourCallsign}, Contest: ${config.contestType.name}, Class: ${config.ourClass}, Section: ${config.ourSection}"
    
    val localConfigExists = contestManager.configExists
    val discoveryConsistent =
      discovered.isEmpty || discovered.values.forall(_ == contestManager.config)

    val newProblems = scala.collection.mutable.ListBuffer[String]()
    if !localConfigExists then newProblems += "No local configuration found"
    if !discoveryConsistent then newProblems += "Inconsistent with other nodes"
    
    problems.clear()
    problems ++= newProblems
    details.value = if discovered.nonEmpty then
      s"$currentDetails (Found ${discovered.size} other nodes)"
    else currentDetails
