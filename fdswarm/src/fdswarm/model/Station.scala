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
 */

package fdswarm.model

import fdswarm.fx.bands.{AvailableBandsStore, HamBand}
import fdswarm.fx.caseForm.ChoiceField
import fdswarm.model.BandMode.*
import upickle.default.*

/**
 * Details about this station.
 *
 * NOTE: Station is now a UI-friendly model (has a ChoiceField),
 * so JSON persistence should use Station.Persisted (below).
 */
final case class Station(
                          band:     ChoiceField[HamBand],
                          modeName: Mode,
                          rig:      String,
                          antenna:  String,
                          operator: Callsign
                        ):
  def bandName: String =
    band.value.bandName

  val bandMode: BandMode =
    BandMode(bandName, modeName)

object Station:

  import Callsign.given_Conversion_String_Callsign

  /** JSON-friendly shape (no functions inside). */
  final case class Persisted(
                              bandName: String,
                              modeName: Mode,
                              rig:      String,
                              antenna:  String,
                              operator: Callsign
                            ) derives ReadWriter

  /** Convert runtime Station -> JSON-friendly Persisted. */
  def toPersisted(s: Station): Persisted =
    Persisted(
      bandName = s.band.value.bandName,
      modeName = s.modeName,
      rig      = s.rig,
      antenna  = s.antenna,
      operator = s.operator
    )

  /**
   * Convert Persisted -> Station by re-attaching the ChoiceField from AvailableBandsStore.
   *
   * If bandName isn't found, falls back to whatever the store selects by default.
   */
  def fromPersisted(p: Persisted)(using store: AvailableBandsStore): Station =
    val selectedBand: HamBand =
      val cb = store.hamBandComboBox(Some(p.bandName))
      val b  = cb.value.value
      if b != null then b else store.hamBandComboBox(None).value.value

    Station(
      band     = store.hamBandChoice(selectedBand),
      modeName = p.modeName,
      rig      = p.rig,
      antenna  = p.antenna,
      operator = p.operator
    )

  /** Convenient default Station (requires AvailableBandsStore so the ChoiceField can be built). */
  def defaultStation(using store: AvailableBandsStore): Station =
    val defaultBand = store.hamBandComboBox(Some("160m")).value.value
    Station(
      band     = store.hamBandChoice(defaultBand),
      modeName = "CW",
      rig      = "Rig 1",
      antenna  = "Antenna 1",
      operator = "WA9NNN"
    )