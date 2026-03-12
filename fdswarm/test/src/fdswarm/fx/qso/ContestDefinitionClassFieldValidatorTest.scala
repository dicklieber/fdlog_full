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

package fdswarm.fx.qso

import munit.FunSuite

class ContestDefinitionClassFieldValidatorTest extends FunSuite:

  def isValid(str: String, classChars: String): Boolean =
    val pattern = "^[0-9]{1,2}[" + classChars.toUpperCase + "]$"
    str.matches(pattern)

  test("isValid validates WFD classes (1-2 digits + value)") {
    val classChars = "HOI" // Typical WFD classes
    assert(isValid("1H", classChars))
    assert(isValid("2O", classChars))
    assert(isValid("12I", classChars))
    assert(isValid("5H", classChars))
  }

  test("isValid validates ARRL classes (1-2 digits + value)") {
    val classChars = "ABCDEFT" // Typical ARRL classes
    assert(isValid("1A", classChars))
    assert(isValid("2B", classChars))
    assert(isValid("12C", classChars))
    assert(isValid("3F", classChars))
  }

  test("isValid rejects invalid classes") {
    val classChars = "ABC"
    assert(!isValid("", classChars))
    assert(!isValid("1", classChars))
    assert(!isValid("A", classChars))
    assert(!isValid("123A", classChars))
    assert(!isValid("1AA", classChars))
    assert(!isValid("1D", classChars)) // D is not in ABC
  }

  def typingPatternMatch(str: String, classChars: String): Boolean =
    val typingPattern = "^([0-9]{1,2}[" + classChars.toUpperCase + "]|[0-9]{0,2})$"
    str.matches(typingPattern)

  test("typingPattern allows building up valid classes") {
    val classChars = "ABC"
    assert(typingPatternMatch("", classChars))
    assert(typingPatternMatch("1", classChars))
    assert(typingPatternMatch("12", classChars))
    assert(typingPatternMatch("1A", classChars))
    assert(typingPatternMatch("12B", classChars))
  }

  test("typingPattern rejects invalid partials") {
    val classChars = "ABC"
    assert(!typingPatternMatch("A", classChars))
    assert(!typingPatternMatch("123", classChars))
    assert(!typingPatternMatch("1D", classChars))
    assert(!typingPatternMatch("1AB", classChars))
  }
