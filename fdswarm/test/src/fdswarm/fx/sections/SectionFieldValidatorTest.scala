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

package fdswarm.fx.sections

import munit.FunSuite

import fdswarm.JavaFxTestKit

class SectionFieldValidatorTest extends FunSuite:

  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  lazy val sections = Seq(
    Section("IL", "Illinois"),
    Section("QC", "Quebec"),
    Section("DX", "DX"),
    Section("SJV", "San Joaquin Valley")
  )

  def isValid(str: String): Boolean =
    if str == null then false
    else
      val upper = str.trim.toUpperCase
      sections.exists(_.code.toUpperCase == upper)

  def isValidPartial(newText: String): Boolean =
    if (newText.isEmpty) true
    else {
      val upper = newText.trim.toUpperCase
      sections.exists(_.code.toUpperCase.startsWith(upper))
    }

  test("isValid validates against section list") {
    assert(isValid("IL"))
    assert(isValid("QC"))
    assert(isValid("DX"))
    assert(isValid("SJV"))
    assert(isValid("il")) // case insensitive
    assert(isValid(" sjv ")) // with spaces
  }

  test("isValid rejects invalid sections") {
    assert(!isValid(""))
    assert(!isValid("XX"))
    assert(!isValid("ILL"))
    assert(!isValid(null))
  }

  test("isValidPartial allows valid prefixes") {
    assert(isValidPartial(""))
    assert(isValidPartial("I"))
    assert(isValidPartial("IL"))
    assert(isValidPartial("S"))
    assert(isValidPartial("SJ"))
    assert(isValidPartial("SJV"))
  }

  test("isValidPartial rejects invalid prefixes") {
    assert(!isValidPartial("X"))
    assert(!isValidPartial("ILL"))
    assert(!isValidPartial("SVA"))
  }
