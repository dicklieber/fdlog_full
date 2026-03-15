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
import fdswarm.fx.contest.ContestStation
import fdswarm.util.NodeIdentity
import scalafx.stage.Window
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager}

@Singleton
class BandModeCondition @Inject()(
    private val bandsManager: AvailableBandsManager,
    private val modesManager: AvailableModesManager,
    private val pane: fdswarm.fx.bandmodes.BandsAndModesPane
) extends StartupCondition:
  override def name: String = "Bands/Modes"

  override def editButton(ownerWindow: Window): Unit =
    pane.show(ownerWindow)

  override def update(discovered: Map[NodeIdentity, ContestStation]): Unit =
    val bands = bandsManager.bands.toSeq
    val modes = modesManager.modes.toSeq
    val totalEnabled = bands.size * modes.size

    val newProblems = scala.collection.mutable.ListBuffer[String]()
    if totalEnabled < 1 then
      newProblems += "Need at least one Band/Mode!"

    problems.clear()
    problems ++= newProblems
    details.value = s"Bands: ${bands.mkString(", ")}, Modes: ${modes.mkString(", ")} ($totalEnabled combinations)"
