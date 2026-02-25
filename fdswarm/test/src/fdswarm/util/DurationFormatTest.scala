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

package fdswarm.util

import java.time.Duration
import munit.FunSuite

class DurationFormatTest extends FunSuite:

  test("format milliseconds"):
    assertEquals(DurationFormat(Duration.ofMillis(500)), "500 ms")

  test("format exact one second"):
    assertEquals(DurationFormat(Duration.ofSeconds(1)), "1 sec")

  test("format seconds and milliseconds"):
    assertEquals(DurationFormat(Duration.ofMillis(1500)), "1 sec 500 ms")

  test("format exact one minute"):
    assertEquals(DurationFormat(Duration.ofMinutes(1)), "1 min")

  test("format minutes and seconds"):
    assertEquals(DurationFormat(Duration.ofMinutes(5).plusSeconds(30)), "5 min 30 sec")

  test("format hours and minutes"):
    assertEquals(DurationFormat(Duration.ofHours(2).plusMinutes(15)), "2 hours 15 min")

  test("format days, hours and minutes"):
    assertEquals(DurationFormat(Duration.ofDays(1).plusHours(2).plusMinutes(3)), "1 day 2 hour 3 min")

  test("format days and hours (no minutes)"):
    assertEquals(DurationFormat(Duration.ofDays(2).plusHours(5)), "2 day 5 hour")
