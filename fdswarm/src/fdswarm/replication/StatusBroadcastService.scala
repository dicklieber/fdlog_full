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
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationConfigManager
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.io.DirectoryProvider
import fdswarm.model.BandModeOperator
import fdswarm.store.QsoStore
import fdswarm.util.NodeIdentityManager
import io.circe.Codec
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.{BooleanProperty, IntegerProperty, StringProperty}

final case class StatusBroadcastSettings(
    periodicEnabled: Boolean = true,
    broadcastPeriodSec: Option[Int] = Option(10)
) derives Codec.AsObject

@Singleton
class StatusBroadcastService @Inject()(
                                        qsoStore: QsoStore,
                                        transport: Transport,
                                        stationManager: StationConfigManager,
                                        selectedBandModeStore:SelectedBandModeManager,
                                        nodeIdentityManager: NodeIdentityManager,
                                        dirProvider: DirectoryProvider,
                                        @Named("fdswarm.broadcastPeriodSec") val defaultBroadcastPeriodSec: Int
                                      ) extends LazyLogging:

  private val settingsPath: os.Path = dirProvider() / "status-broadcast-settings.json"

  private def loadSettings(): StatusBroadcastSettings =
    if os.exists(settingsPath) then
      decode[StatusBroadcastSettings](os.read(settingsPath)).getOrElse(StatusBroadcastSettings())
    else
      StatusBroadcastSettings()

  private def saveSettings(): Unit =
    val settings = StatusBroadcastSettings(
      periodicEnabled = periodicEnabledProperty.value,
      broadcastPeriodSec = Some(broadcastPeriodSecProperty.value)
    )
    val json = settings.asJson.spaces2
    os.write.over(settingsPath, json, createFolders = true)

  private val initialSettings = loadSettings()

  val periodicEnabledProperty: BooleanProperty = new BooleanProperty(this, "periodicEnabled", initialSettings.periodicEnabled)

  val broadcastPeriodSecProperty: IntegerProperty = new IntegerProperty(this, "broadcastPeriodSec", initialSettings.broadcastPeriodSec.getOrElse(defaultBroadcastPeriodSec))

  periodicEnabledProperty.onChange { (_, _, newValue) =>
    saveSettings()
    if newValue then start() else stop()
  }

  broadcastPeriodSecProperty.onChange { (_, _, _) =>
    saveSettings()
    // Nudge the running scheduler so the new period controls the NEXT delay
    // without forcing an immediate broadcast.
    interruptForReschedule()
  }

  @volatile private var maybeThread: Option[Thread] = None
  @volatile private var stopRequested: Boolean = false

  /** Interrupt the scheduler thread (if any) to re-evaluate its sleep period.
    * This does NOT stop the service; it only short-circuits the current sleep
    * so the next wait uses the freshly updated period. */
  private def interruptForReschedule(): Unit =
    this.synchronized {
      maybeThread.foreach(_.interrupt())
    }

  def start(): Unit =
    this.synchronized {
      if maybeThread.isEmpty && periodicEnabledProperty.value then
        stopRequested = false
        val period = broadcastPeriodSecProperty.value
        logger.debug(s"Starting periodic Status broadcasts (every $period s)")
        val t = new Thread(() => {
          // Optionally broadcast immediately on start (keeps previous behavior)
          try
            broadcastStatus()
          catch
            case e: Exception => logger.warn("Initial status broadcast failed", e)

          // Scheduler: sleep for the current period, then broadcast.
          // If interrupted and not stopping, we simply re-check the (possibly new) period
          // and sleep again, so the next broadcast is delayed by the updated period.
          var skipNextBroadcast = false
          while !Thread.currentThread().isInterrupted do
            try
              val currentPeriod = broadcastPeriodSecProperty.value
              Thread.sleep(currentPeriod * 1000L)
              // Woke normally: proceed to broadcast unless a prior interrupt set skip flag
              if !skipNextBroadcast then
                broadcastStatus()
              else
                skipNextBroadcast = false
            catch
              case _: InterruptedException =>
                // Distinguish between a stop request and a reschedule nudge
                if stopRequested then
                  // Exit loop
                  Thread.currentThread().interrupt()
                else
                  // Reschedule: skip the immediate broadcast; go sleep with new period
                  skipNextBroadcast = true
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
    try
      val operator = stationManager.station.operator
      val bandMode = selectedBandModeStore.selected.value
      val bandModeOperator = BandModeOperator(operator,bandMode )
      val statusMessage = StatusMessage( fdDigests = qsoStore.digests(), bandModeOperator)
      val gzipBytes = statusMessage.toPacket
      logger.trace("Broadcasting status: {} bytes: {}", statusMessage, gzipBytes.length)
//      transport.send(Service.Status, gzipBytes)
    catch
      case e: Exception =>
        logger.error("Error broadcasting node status", e)
