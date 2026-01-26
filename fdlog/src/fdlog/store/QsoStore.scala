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
import fdlog.model.*
import fdlog.util.GenerateId.Id
import jakarta.inject.*

import scala.collection.concurrent.TrieMap

@Singleton
class QsoStore:
  private val map: TrieMap[Id, Qso] = new TrieMap
  
  def load(qsos:Iterator[Qso]):Unit=
    qsos.foreach(qso => map.put(qso.uuid, qso))

  def ids: Seq[Id] =
    val byTie = map.values.toSeq.sorted
    byTie.map(_.uuid)

  
