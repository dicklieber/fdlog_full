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

package fdswarm

import java.time.*

final case class ContestDates(startUtc: ZonedDateTime, endUtc: ZonedDateTime)

object ContestDateCalculator:
  val Epoch: ContestDates =
    ContestDates(
      startUtc = Instant.EPOCH.atZone(ZoneOffset.UTC),
      endUtc = Instant.EPOCH.atZone(ZoneOffset.UTC)
    )

  def none(year: Int): ContestDates = Epoch

  
  /**
   * WFD
   * @param year for the event.
   * @return start and end UTC ZonedDateTimes for the contest.
   */
  def lastFull(year: Int): ContestDates =
    val (sat, sun) = lastFullWeekend(year, Month.JANUARY)
    ContestDates(
      startUtc = sat.atTime(19, 0).atZone(ZoneOffset.UTC), // 1900Z Sat
      endUtc = sun.atTime(18, 59).atZone(ZoneOffset.UTC) // 1859Z Sun
    )

  /**
   * ARRL
   *
   * @param year for the event.
   * @return
   */
  def forthFullWeekend(year: Int): ContestDates =
    val (sat, sun) = nthFullWeekend(year, Month.JUNE, n = 4) // 4th full weekend in June
    ContestDates(
      startUtc = sat.atTime(18, 0).atZone(ZoneOffset.UTC), // 1800Z Sat
      endUtc = sun.atTime(20, 59).atZone(ZoneOffset.UTC) // 2059Z Sun
    )

  // ---- Helpers --------------------------------------------------------------

  /** Nth full weekend (Sat/Sun both in the same month), n is 1-based. */
  def nthFullWeekend(year: Int, month: Month, n: Int): (LocalDate, LocalDate) =
    require(n >= 1, s"n must be >= 1, got $n")

    val (firstSat, firstSun) = firstFullWeekend(year, month)
    (firstSat.plusWeeks((n - 1).toLong), firstSun.plusWeeks((n - 1).toLong))

  /** First full weekend (Sat/Sun both in the same month). */
  private def firstFullWeekend(year: Int, month: Month): (LocalDate, LocalDate) =
    val firstDay = LocalDate.of(year, month, 1)

    val daysToSat =
      (DayOfWeek.SATURDAY.getValue - firstDay.getDayOfWeek.getValue + 7) % 7

    val sat = firstDay.plusDays(daysToSat.toLong)
    val sun = sat.plusDays(1)

    (sat, sun)

  /** Last full weekend (Sat/Sun both in the same month). */
  def lastFullWeekend(year: Int, month: Month): (LocalDate, LocalDate) =
    val lastDay = LocalDate.of(year, month, month.length(Year.isLeap(year.toLong)))

    val daysBackToSat =
      (lastDay.getDayOfWeek.getValue - DayOfWeek.SATURDAY.getValue + 7) % 7

    var sat = lastDay.minusDays(daysBackToSat.toLong)
    var sun = sat.plusDays(1)

    // If Sunday spills into the next month, go back a week
    if sun.getMonth != month then
      sat = sat.minusWeeks(1)
      sun = sat.plusDays(1)

    (sat, sun)
