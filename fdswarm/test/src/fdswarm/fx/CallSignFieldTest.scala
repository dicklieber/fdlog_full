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

import munit.FunSuite

class CallSignFieldTest extends FunSuite:

  test("CallSignField.isValid validates standard callsigns") {
    assert(CallSignField.isValid("K1ABC"))
    assert(CallSignField.isValid("WA9NNN"))
    assert(CallSignField.isValid("G4XYZ"))
    assert(CallSignField.isValid("7J1RL"))
    assert(CallSignField.isValid("N0ABC"))
  }


  test("CallSignField.isValid rejects invalid callsigns") {
    assert(!CallSignField.isValid("")) // Empty
    assert(!CallSignField.isValid("K")) // Too short (regex requires digit as 2nd, 3rd or 4th char)
    assert(!CallSignField.isValid("K1ABCDEFG HIJKL")) // Contains space
    assert(!CallSignField.isValid("ABCDEFG")) // No digit
    assert(!CallSignField.isValid("K1ABC/")) // Trailing slash
    assert(!CallSignField.isValid("/K1ABC")) // Leading slash
    assert(!CallSignField.isValid("VE3/K1ABC")) // Suffix too long (>4)
  }

  test("CallSignField.isValid respects regex-defined length limits") {
    // [A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4} -> max 8 chars
//    assert(CallSignField.isValid("ABC0ABCD")) // 8 chars - max for main part
    assert(!CallSignField.isValid("ABC0ABCDE")) // 9 chars - too long for main part

    // With suffix: (?=.{3,12}$) restricts total length to 12
    assert(CallSignField.isValid("ABC0ABCD/P")) // 10 chars
    // ABC0ABCD/123 is 8+1+3 = 12.
    assert(CallSignField.isValid("ABC0ABCD/123")) // 12 chars - max for lookahead
    // ABC0ABCD/1234 is 8+1+4 = 13.
    assert(!CallSignField.isValid("ABC0ABCD/1234")) // 13 chars - too long for lookahead

    assert(CallSignField.isValid("K1A")) // 3 chars - min for lookahead
  }
    