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

package fdswarm.client

import fdswarm.model.{BandMode, Qso}
import fdswarm.store.QsoStore
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import cats.effect.unsafe.implicits.global
import fdswarm.io.DirectoryProvider

@Singleton
class RestQsoStore @Inject()(
    restClient: FdSwarmRestClient,
    directoryProvider: DirectoryProvider,
    registry: MeterRegistry
) extends QsoStore(directoryProvider, registry)
    with LazyLogging:

  // Disable journal loading from local file if we want it to be pure REST,
  // but QsoStore's constructor does it.
  // For a client, we might want to just fetch from the server.

  def refreshLastQsos(n: Int): Unit =
    restClient.getLastQsos(n).unsafeRunAsync {
      case Right(apiResponse) =>
        scalafx.application.Platform.runLater {
          // Clear and add new ones to be sure we match server's "last N"
          qsoCollection.clear()
          apiResponse.data.foreach { qso =>
            map.put(qso.uuid, qso)
            qsoCollection.add(qso) // QsoStore uses prepend, but for "last N" maybe we want order
          }
        }
      case Left(error) =>
        logger.error(s"Failed to refresh last QSOs: ${error.getMessage}")
    }

  override def add(batch: Seq[Qso]): Unit =
    // In the client, adding a QSO means POSTing it to the server
    batch.foreach { qso =>
      restClient.postQso(qso).unsafeRunAsync {
        case Right(_) =>
          logger.debug(s"Successfully posted QSO: ${qso.uuid}")
          // Optionally add to local collection immediately or wait for refresh
          scalafx.application.Platform.runLater {
             if !map.contains(qso.uuid) then
                map.put(qso.uuid, qso)
                qsoCollection.prepend(qso)
          }
        case Left(error) =>
          logger.error(s"Failed to post QSO: ${error.getMessage}")
      }
    }

  override def potentialDups(startOfCallsign: String, bandmode: BandMode): Seq[Qso] =
    // Try local first
    val local = super.potentialDups(startOfCallsign, bandmode)
    
    // Trigger async fetch to update local cache
    restClient.getPotentialDups(startOfCallsign, bandmode).unsafeRunAsync {
      case Right(resp) =>
        scalafx.application.Platform.runLater {
          resp.data.foreach { qso =>
            if !map.contains(qso.uuid) then
              map.put(qso.uuid, qso)
              qsoCollection.prepend(qso)
          }
        }
      case Left(error) =>
        logger.error(s"Failed to fetch potential dups from server: ${error.getMessage}")
    }
    local
