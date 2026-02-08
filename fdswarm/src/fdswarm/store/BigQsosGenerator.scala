/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later.
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

package fdswarm.store

import fdswarm.fx.contest.ContestType.WFD
import fdswarm.model.*
import jakarta.inject.*

@Singleton
final class BigQsosGenerator @Inject()(qsoStore: QsoStore):

  /** Generate synthetic QSOs and *immediately* add them to QsoStore.
   *
   * IMPORTANT: Iterator#map is lazy; we must consume the iterator or nothing happens.
   */
    val exchange = Exchange(FdClass(1, 'I'), "IL")
    val bandMode  = BandMode("20M", "PH")

    def qsos(howMany: Int, prefix: String): Unit =
      callsignIterator(prefix)
        .take(howMany)
        .foreach { callSign =>
          val qsoMetadata = QsoMetadata(station = Station(), contest = WFD)
          val qso = Qso(Callsign(callSign), "1H", "IL", bandMode, qsoMetadata)
          qsoStore.add(qso)
        }

    def callsignIterator(prefix: String): Iterator[String] =
      for
        a <- Iterator.from('A'.toInt).map(_.toChar)
        b <- Iterator.from('A'.toInt).map(_.toChar)
        c <- Iterator.from('A'.toInt).map(_.toChar)
      yield s"$prefix$a$b$c"
