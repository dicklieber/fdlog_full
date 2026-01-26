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

package fdlog.model


import fdlog.util.GenerateId.*
import fdlog.util.JavaTimePickle.given
import upickle.ReadWriter

import java.time.Instant


/**
 * This is what's in the store and journal.log.
 *
 * @param callSign    of the worked station.
 * @param exchange    from the worked station.
 * @param bandMode    that was used.
 * @param stamp       when QSO occurred.
 * @param uuid        id unique QSO id in time & space.
 * @param qsoMetadata info about ur station.
 */
case class Qso(callSign: CallSign,
               exchange: Exchange,
               bandMode: BandMode,
               qsoMetadata: QsoMetadata,
               stamp: Instant = Instant.now(),
               uuid: Id = generateId()) derives ReadWriter:
  def isDup(that: Qso): Boolean =
    this.callSign == that.callSign &&
      this.bandMode == that.bandMode

object Qso:
  given Ordering[Qso] = Ordering.by(_.stamp)

type CallSign = String




