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

import com.typesafe.scalalogging.LazyLogging
import jakarta.inject.{Inject, Provider, Singleton}
import scalafx.beans.property.StringProperty

import java.util.concurrent.{CopyOnWriteArrayList, LinkedBlockingQueue}

@Singleton
class SwitchingTransport @Inject() (
    multicastProvider: Provider[MulticastTransport],
    broadcastProvider: Provider[BroadcastTransport]
) extends Transport
    with LazyLogging:

  val transportTypeProperty = StringProperty("Multicast")

  private var activeTransport: Transport = multicastProvider.get()
  val queue = new LinkedBlockingQueue[UDPHeaderData]()
  private val listeners = new CopyOnWriteArrayList[UDPHeaderData => Unit]()

  private val internalListener: UDPHeaderData => Unit = data => {
    queue.offer(data)
    listeners.forEach(_.apply(data))
  }

  activeTransport.addListener(internalListener)

  transportTypeProperty.onChange { (_, _, newValue) =>
    synchronized {
      logger.info(s"Switching transport to $newValue")
      activeTransport.removeListener(internalListener)
      // Note: We don't stop the previous transport here as they are singletons managed by Guice,
      // but they might continue receiving in the background. 
      // For now, we just switch where we get our data from and where we send to.
      
      activeTransport = newValue match
        case "Broadcast" => broadcastProvider.get()
        case _           => multicastProvider.get()
      
      activeTransport.addListener(internalListener)
    }
  }

  override def addListener(listener: UDPHeaderData => Unit): Unit = listeners.add(listener)
  override def removeListener(listener: UDPHeaderData => Unit): Unit = listeners.remove(listener)

  override def send(data: Array[Byte]): Unit = synchronized {
    activeTransport.send(data)
  }

  override def send(service: Service, data: Array[Byte]): Unit = synchronized {
    activeTransport.send(service, data)
  }

  override def stop(): Unit = synchronized {
    // This is called on app shutdown, we should probably stop both if they were started.
    // However, Guice might handle their lifecycle if they are singletons.
    multicastProvider.get().stop()
    broadcastProvider.get().stop()
  }
