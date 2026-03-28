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

import munit.FunSuite

class CamelToWordsTest extends FunSuite:

  test("contestType -> Contest Type") {
    assertEquals(camelToWords("contestType"), "Contest Type")
  }

  test("transmitters -> Transmitters") {
    assertEquals(camelToWords("transmitters"), "Transmitters")
  }

  test("ourCallsign -> Our Callsign") {
    assertEquals(camelToWords("ourCallsign"), "Our Callsign")
  }

  test("ourClass -> Our Class") {
    assertEquals(camelToWords("ourClass"), "Our Class")
  }

  test("ourSection -> Our Section") {
    assertEquals(camelToWords("ourSection"), "Our Section")
  }

  test("empty string -> empty") {
    assertEquals(camelToWords(""), "")
  }

  test("single lowercase -> single uppercase") {
    assertEquals(camelToWords("a"), "A")
  }

  test("single uppercase -> single uppercase") {
    assertEquals(camelToWords("A"), "A")
  }

  test("all lowercase -> first upper rest lower") {
    assertEquals(camelToWords("abc"), "Abc")
  }

  test("all uppercase -> first upper rest lower") {
    assertEquals(camelToWords("ABC"), "Abc")
  }

  test("multiple camel humps httpResponse -> Http Response") {
    assertEquals(camelToWords("httpResponse"), "Http Response")
  }