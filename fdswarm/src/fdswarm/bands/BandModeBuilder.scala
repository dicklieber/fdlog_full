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

package fdswarm.bands

import fdswarm.model.BandMode
import fdswarm.model.Band
import fdswarm.model.BandMode.Mode
import jakarta.inject.{Inject, Singleton}

@Singleton
class BandModeBuilder @Inject()(modeCatalog: ModeCatalog):
  private val knownBands: Set[Band] = Band.values.toSet

  def apply(band: Band, mode: Mode): BandMode =
    val normalizedMode = mode.toUpperCase

    val bandExists = knownBands.contains(band)
    val modeExists = modeCatalog.modes.exists(_.toUpperCase == normalizedMode)

    if !bandExists then
      throw new IllegalArgumentException(s"Band '$band' not found in Band enum")
    
    if !modeExists then
      throw new IllegalArgumentException(s"Mode '$mode' not found in ModeCatalog")

    new BandMode(band, normalizedMode)
