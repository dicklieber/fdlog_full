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

import munit.FunSuite
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.time.DayOfWeek

import fdswarm.model.Contest

final class ContestDateCalculatorTest extends FunSuite:

  private def zdtUtc(d: LocalDate, h: Int, m: Int): ZonedDateTime =
    d.atTime(h, m).atZone(ZoneOffset.UTC)

  test("WFD 2026: last full weekend of January + correct UTC window"):
    val cd = ContestDateCalculator.datesFor(Contest.WFD, 2026)

    assertEquals(cd.startUtc.toLocalDate, LocalDate.of(2026, 1, 24))
    assertEquals(cd.endUtc.toLocalDate, LocalDate.of(2026, 1, 25))

    assertEquals(cd.startUtc.getDayOfWeek, DayOfWeek.SATURDAY)
    assertEquals(cd.endUtc.getDayOfWeek, DayOfWeek.SUNDAY)

    assertEquals(cd.startUtc, zdtUtc(LocalDate.of(2026, 1, 24), 19, 0))   // 1900Z Sat
    assertEquals(cd.endUtc,   zdtUtc(LocalDate.of(2026, 1, 25), 18, 59))  // 1859Z Sun

  test("ARRL 2026: 4th full weekend of June + correct UTC window"):
    val cd = ContestDateCalculator.datesFor(Contest.ARRL, 2026)

    assertEquals(cd.startUtc.toLocalDate, LocalDate.of(2026, 6, 27))
    assertEquals(cd.endUtc.toLocalDate, LocalDate.of(2026, 6, 28))

    assertEquals(cd.startUtc.getDayOfWeek, DayOfWeek.SATURDAY)
    assertEquals(cd.endUtc.getDayOfWeek, DayOfWeek.SUNDAY)

    assertEquals(cd.startUtc, zdtUtc(LocalDate.of(2026, 6, 27), 18, 0))   // 1800Z Sat
    assertEquals(cd.endUtc,   zdtUtc(LocalDate.of(2026, 6, 28), 20, 59))  // 2059Z Sun

  test("Sanity: weekends are Sat/Sun and stay inside their month for several years"):
    val years = Seq(2024, 2025, 2026, 2027)

    years.foreach { y =>
      val wfd = ContestDateCalculator.datesFor(Contest.WFD, y)
      assertEquals(wfd.startUtc.getDayOfWeek, DayOfWeek.SATURDAY)
      assertEquals(wfd.endUtc.getDayOfWeek, DayOfWeek.SUNDAY)
      assertEquals(wfd.startUtc.getMonthValue, 1)
      assertEquals(wfd.endUtc.getMonthValue, 1)

      val arrl = ContestDateCalculator.datesFor(Contest.ARRL, y)
      assertEquals(arrl.startUtc.getDayOfWeek, DayOfWeek.SATURDAY)
      assertEquals(arrl.endUtc.getDayOfWeek, DayOfWeek.SUNDAY)
      assertEquals(arrl.startUtc.getMonthValue, 6)
      assertEquals(arrl.endUtc.getMonthValue, 6)
    }
