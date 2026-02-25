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

package fdswarm.fx

import fdswarm.model.Callsign
import munit.FunSuite

class CallSignFieldTest extends FunSuite:

  test("Callsign.isValid validates standard callsigns") {
    assert(Callsign.isValid("K1ABC"))
    assert(Callsign.isValid("WA9NNN"))
    assert(Callsign.isValid("G4XYZ"))
    assert(Callsign.isValid("7J1RL"))
    assert(Callsign.isValid("N0ABC"))
  }


  test("Callsign.isValid rejects invalid callsigns") {
    assert(!Callsign.isValid("")) // Empty
    assert(!Callsign.isValid("K")) // Too short (regex requires digit as 2nd, 3rd or 4th char)
    assert(!Callsign.isValid("K1ABCDEFG HIJKL")) // Contains space
    assert(!Callsign.isValid("ABCDEFG")) // No digit
    assert(!Callsign.isValid("K1ABC/")) // Trailing slash
    assert(!Callsign.isValid("/K1ABC")) // Leading slash
    assert(!Callsign.isValid("VE3/K1ABC")) // Suffix too long (>4)
  }

  test("Callsign.isValid respects regex-defined length limits") {
    // [A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4} -> max 8 chars
    //    assert(Callsign.isValid("ABC0ABCD")) // 8 chars - max for main part
    assert(!Callsign.isValid("ABC0ABCDE")) // 9 chars - too long for main part

    // With suffix: (?=.{3,12}$) restricts total length to 12
    assert(Callsign.isValid("ABC0ABCD/P")) // 10 chars
    // ABC0ABCD/123 is 8+1+3 = 12.
    assert(Callsign.isValid("ABC0ABCD/123")) // 12 chars - max for lookahead
    // ABC0ABCD/1234 is 8+1+4 = 13.
    assert(!Callsign.isValid("ABC0ABCD/1234")) // 13 chars - too long for lookahead

    assert(Callsign.isValid("K1A")) // 3 chars - min for lookahead
  }
    