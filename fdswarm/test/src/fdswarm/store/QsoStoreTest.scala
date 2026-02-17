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

package fdswarm.store

import fdswarm.TestDirectory
import fdswarm.model.QsoMetadata.testQsoMetadata
import fdswarm.model.{BandMode, Callsign, Qso, QsoMetadata, Station}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import scala.compiletime.uninitialized

class QsoStoreTest extends FunSuite:
  private var testDirectory: TestDirectory = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  test("FdHour initially empty"):
    val registry = new SimpleMeterRegistry()
    val qsoStore = QsoStore(testDirectory, registry)

    assertEquals(qsoStore.digests().isEmpty, true)
    assertEquals(qsoStore.qsoCollection.isEmpty, true)

  test("add 1st QSO"):
    val registry = new SimpleMeterRegistry()
    val qsoStore = QsoStore(testDirectory, registry)

    val qso = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    qsoStore.add(qso)
    assertEquals(qsoStore.digests().size, 1)
    assertEquals(qsoStore.qsoCollection.size, 1)
    val backAgain = qsoStore.get(qso.uuid)
    assertEquals(backAgain.get, qso)
