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

package fdswarm.api

import fdswarm.io.DirectoryProvider
import fdswarm.model.*
import fdswarm.store.QsoStore
import upickle.default.*
import fdswarm.fx.contest.ContestType

class QsoRoutesTests extends munit.FunSuite {

  test("qsos endpoint returns JSON list of QSOs") {
    // Setup a mock directory provider for QsoStore
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }

    val qsoStore = new QsoStore(directoryProvider)
    val routes = new QsoRoutes(qsoStore)

    // Add some sample QSOs
    val qso1 = Qso(
      callSign = Callsign("W1AW"),
      contestClass = "1A",
      section = "CT",
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = QsoMetadata(Station("S1", "Home", Callsign("WA9NNN")), "node1", ContestType.WFD)
    )
    qsoStore.add(qso1)

    val response = routes.allQsos()

    // Verify content type
    val contentType = response.headers.find(_._1.equalsIgnoreCase("Content-Type")).map(_._2)
    assertEquals(contentType, Some("application/json"))

    val responseData = response.data
    // Verify response is a JSON array and contains the QSO
    assert(responseData.startsWith("[") && responseData.endsWith("]"))
    val qsos = read[Seq[Qso]](responseData)
    assertEquals(qsos.size, 1)
    assertEquals(qsos.head.callSign.value, "W1AW")
  }
}
