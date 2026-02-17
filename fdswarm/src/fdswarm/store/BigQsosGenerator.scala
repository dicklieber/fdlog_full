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

import fdswarm.fx.bands.BandModeBuilder
import fdswarm.fx.contest.ContestType.WFD
import fdswarm.model.*
import jakarta.inject.*

import java.time.Instant

@Singleton
final class BigQsosGenerator @Inject()(qsoStore: QsoStore, bandModeBuilder: BandModeBuilder):

  /** Generate synthetic QSOs and *immediately* add them to QsoStore.
   *
   * IMPORTANT: Iterator#map is lazy; we must consume the iterator or nothing happens.
   */
  private val exchange = Exchange(FdClass())
  private val bandMode = bandModeBuilder("20M", "PH")

  /**
   *
   * @param howMany        qsos to generate.
   * @param howManyPerHour put howMany qsos per hour.
   * @param prefix         for the generated callsigs.
   * @param now            allow unit test to set the starting time, so we get repeatable results.
   */
  def qsos(howMany: Int, howManyPerHour: Int, prefix: String, now: Instant = java.time.Instant.now()): Unit =
    val intervalMillis = (3600L * 1000L) / howManyPerHour
    val generatedCallsigns = callsignIterator(prefix)
      .take(howMany)
      .zipWithIndex
    val batchOfQsos: Seq[Qso] = (for
      (callsign, index) <- generatedCallsigns
    yield
      val stamp = now.minusMillis(index * intervalMillis)
      val qsoMetadata = QsoMetadata(station = Station(), contest = WFD)
      Qso(callsign = Callsign(callsign),
        contestClass = "1H",
        section = "IL",
        bandMode = bandMode,
        qsoMetadata = qsoMetadata,
        stamp = stamp)
//      qsoStore.add(qso)
      ).toSeq
    qsoStore.add(batchOfQsos)


  private def callsignIterator(prefix: String): Iterator[String] =
    val alphabet = 'A' to 'Z'
    for
      a <- alphabet.iterator
      b <- alphabet.iterator
      c <- alphabet.iterator
    yield s"$prefix$a$b$c"
