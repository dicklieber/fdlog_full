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

package fdswarm.replication

import com.google.inject.name.Named
import fdswarm.logging.LazyStructuredLogging
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.model.BandModeOperator
import fdswarm.store.QsoStore
import jakarta.inject.{Inject, Singleton, Provider}

@Singleton
class StatusBroadcastService @Inject()(
                                        qsoStoreProvider: Provider[QsoStore],
                                        transport: Transport,
                                        stationManager: StationConfigManager,
                                        selectedBandModeStore: SelectedBandModeManager,
                                        contestConfigManager: ContestConfigManager,
                                        @Named("fdswarm.broadcastPeriodSec") val defaultBroadcastPeriodSec: Int
                                      ) extends LazyStructuredLogging:

  private def qsoStore: QsoStore = qsoStoreProvider.get()

  @volatile private var maybeThread: Option[Thread] = None
  @volatile private var stopRequested: Boolean = false

  private def currentPeriodMillis: Long =
    defaultBroadcastPeriodSec * 1000L

  def start(): Unit =
    this.synchronized {
      if maybeThread.isEmpty then
        stopRequested = false
        val period = defaultBroadcastPeriodSec
        logger.debug(s"Starting periodic Status broadcasts (every $period s)")
        val t = new Thread(() => {
          while !Thread.currentThread().isInterrupted do
            try
              Thread.sleep(currentPeriodMillis)
              broadcastStatus()
            catch
              case _: InterruptedException =>
                if stopRequested then
                  // Exit loop
                  Thread.currentThread().interrupt()
        }, "Status-Broadcaster")
        t.setDaemon(true)
        t.start()
        maybeThread = Some(t)
    }

  def stop(): Unit =
    this.synchronized {
      logger.debug("Stopping periodic Status broadcasts")
      stopRequested = true
      maybeThread.foreach(_.interrupt())
      maybeThread = None
    }

  def broadcastStatus(): Unit =
    if !contestConfigManager.hasConfiguration.value then return
    try
      val operator = stationManager.station.operator
      val bandMode = selectedBandModeStore.selected.value
      val bandModeOperator = BandModeOperator(
        operator,
        bandMode
      )
      val statusMessage = StatusMessage(hash = qsoStore.idsHash,
        bandNodeOperator = bandModeOperator,
        contestConfig = contestConfigManager.contestConfigProperty.value, qsoStore.size)
      val gzipBytes = statusMessage.toPacket
      transport.send(Service.Status, gzipBytes)
    catch
      case e: Exception =>
        logger.error("Error broadcasting node status", e)
