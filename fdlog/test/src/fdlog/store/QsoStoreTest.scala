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

package fdlog.store

import fdlog.model.Qso
import fdlog.util.Ids
import fdlog.util.Ids.Id
import munit.FunSuite

class QsoStoreTest extends FunSuite:
  val howMany = 10
  val qsos: Iterator[Qso] = BigQsosGenerator.qsos(howMany)
  test("ids have/need"):
    val qsoStore = new QsoStore
    qsoStore.load(qsos)

    val ids: String = qsoStore.ids
    val idsInStore = qsoStore.neededIds(ids)
    assert(idsInStore.isEmpty)
    val qsoSize = qsoStore.size
    assertEquals(qsoSize, howMany)
    // now add one more
    val newQso = Qso("WA9NNN")

    val fromAnotherNode:String = ids +  newQso.uuid
    val neededIds: Seq[Id] = qsoStore.neededIds(fromAnotherNode)
    assertEquals(neededIds.size, 1)
    assertEquals(neededIds.head, newQso.uuid)




