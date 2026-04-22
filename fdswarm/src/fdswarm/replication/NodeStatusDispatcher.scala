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

import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented

import scala.collection.concurrent.TrieMap

/**
 * A singleton class that acts as a dispatcher for handling and dispatching node status updates.
 * It listens for UDP packets received by the transport layer, processes incoming data, and notifies
 * registered listeners based on the associated service.
 *
 * This class also provides a mechanism to register listeners for specific services.
 *
 * @constructor Creates an instance of `NodeStatusDispatcher`
 *              and starts a dedicated background thread to process incoming UDP packets.
 * @param transport The transport instance used for receiving and dispatching UDP packets.
 *
 *                  This dispatcher handles packets through the following mechanism:
 *                  - Listens for incoming UDP packets using the transport's queue.
 *                  - Decodes the packets and determines the service and payload.
 *                  - Notifies registered listeners (if any) associated with the specified service for the packet.
 *
 *                  The class maintains an internal thread for processing packets continuously until the thread is interrupted.
 *
 *                  Metrics:
 *                  - Tracks the total number of received packets.
 *                  - Tracks the size of the payload for the last received "Status" message.
 *
 *                  Thread Safety:
 *                  - Listener registration is thread-safe, ensuring that multiple threads can register listeners without collisions.
 *
 *                  Listener Registration:
 *                  - Listeners are registered per `Service` type.
 *                  - Supports both single and multiple listener modes.
 *   - In single-listener mode, only one listener can be registered for a service.
 *   - In multi-listener mode, multiple listeners can be registered for a single service.
 *
 * Exception Handling:
 *                  - Logs errors that occur during the processing of packets or notification of listeners.
 *                  - If the processing thread is interrupted, it exits gracefully. */
@Singleton
class NodeStatusDispatcher @Inject() (transport: Transport) extends LazyStructuredLogging() with DefaultInstrumented:

  private val listenersByService: TrieMap[Service[?], Seq[ListenerRegistration]] = TrieMap.empty
  private val thread = new Thread(
    () =>
      while !Thread.currentThread().isInterrupted do
        try
          val udpHeader: UDPHeaderData = transport.incomingQueue.take()
          logger.trace(s"Received UDP packet from ${udpHeader.nodeIdentity} for service ${udpHeader.service}")
          receivedPacketCount.inc(1)
          if udpHeader.service == Service.Status then lastStatusMessagePayloadSize = udpHeader.payload.length.toDouble
          notifyFromRegistry(udpHeader = udpHeader)
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()

          case e: Exception =>
            e.printStackTrace()
            logger.error(s"Error in Repl processing loop ${e.getMessage}", e)
    ,
    "Repl-Processor"
  )

  private val receivedPacketCount = metrics.counter("received_packets")
  private var lastStatusMessagePayloadSize: Double = 0.0

  def addListener[T](service: Service[T], singleListener: Boolean = true)(listener: (NodeIdentity, T) => Unit): Unit =
    this.synchronized {
      val existing: Seq[ListenerRegistration] = listenersByService.getOrElse(service, Seq.empty)
      if singleListener && existing.nonEmpty then
        throw new IllegalStateException(s"${service.toString} listener already set")
      val registration = new TypedListenerRegistration[T](service = service, listener = listener)
      listenersByService.update(service, existing :+ registration)
    }

  thread.setDaemon(true)
  thread.start()

  private def notifyFromRegistry(udpHeader: UDPHeaderData): Unit =
    val registrations = listenersByService.getOrElse(udpHeader.service, Seq.empty)
    if registrations.isEmpty then
      logger.warn("Dropping because no listener is registered", "Service" -> udpHeader.service)
    else
      registrations.foreach(registration =>
        try registration.handle(udpHeader = udpHeader)
        catch case e: Exception => logger.error(s"Error in ${registration.service} listener", e))

  private trait ListenerRegistration:
    def service: Service[?]
    def handle(udpHeader: UDPHeaderData): Unit

  private final class TypedListenerRegistration[T](val service: Service[T], listener: (NodeIdentity, T) => Unit)
      extends ListenerRegistration:
    private val receivedCounter = metrics.counter(s"received.$service")

    override def handle(udpHeader: UDPHeaderData): Unit =
      val value: T = udpHeader.decodeFor(expectedService = service)
      receivedCounter.inc(1)
      listener(udpHeader.nodeIdentity, value)
