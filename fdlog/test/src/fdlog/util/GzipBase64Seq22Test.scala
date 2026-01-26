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

package fdlog.util

import fdlog.model.Qso
import fdlog.store.{BigQsosGenerator, QsoStore}
import munit.FunSuite

class GzipBase64Seq22Test extends FunSuite:
  val howMany = 10000
  val qsos: Iterator[Qso] = BigQsosGenerator.qsos(howMany)

  test("happy path"):
    val qsoStore = new QsoStore()
    qsoStore.load(qsos)
    val ids = qsoStore.ids
    val noGzipString = ids.mkString
    val noGzipSize = noGzipString.length
    val base64Seq22 = GzipBase64Seq22.encode(ids)
    val backAgain = GzipBase64Seq22.decode(base64Seq22)
    assertEquals(backAgain, ids)
    val gzipSize = base64Seq22.length
    println(s"noGzipSize: $noGzipSize, gzipSize: $gzipSize savings: ${noGzipSize - gzipSize}")

