package fdswarm.replication

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import java.net.*
import java.util.concurrent.LinkedBlockingQueue
import scala.jdk.CollectionConverters.*

@Singleton
class NodeStatusReceiver @Inject()(
                                    @Named("fdswarm.statusPort") statusPort: Int
                                  ) extends LazyLogging:

  val queue = new LinkedBlockingQueue[Array[Byte]]()

  private var socket: Option[DatagramSocket] = None
  private var thread: Option[Thread] = None

  private lazy val myAddresses: Set[InetAddress] =
    NetworkInterface.getNetworkInterfaces.asScala
      .flatMap(_.getInetAddresses.asScala)
      .toSet

  def start(): Unit =
    logger.info(s"Starting NodeStatus receiver on port $statusPort")

    val s = new DatagramSocket(null)
    s.setReuseAddress(true)
    s.bind(new InetSocketAddress(statusPort))
    socket = Some(s)

    val t = new Thread(
      () =>
        val buffer = new Array[Byte](65535)

        while !Thread.currentThread().isInterrupted do
          try
            val packet = new DatagramPacket(buffer, buffer.length)
            s.receive(packet)

            val sender = packet.getAddress

            if myAddresses.contains(sender) then
              logger.debug(s"Ignoring our own status message from $sender")
            else
              val receivedData = new Array[Byte](packet.getLength)
              System.arraycopy(
                packet.getData,
                packet.getOffset,
                receivedData,
                0,
                packet.getLength
              )

              try
                val headerData = UDPHeader.parse(receivedData)
                if headerData.service == Service.Status then
                  queue.offer(headerData.payload)
                  logger.trace(
                    s"Received and queued status from $sender: ${headerData.payload.length} bytes"
                  )
                else
                  logger.debug(
                    s"Received non-status message on status port from $sender: ${headerData.service}"
                  )
              catch
                case e: IllegalArgumentException =>
                  logger.debug(
                    s"Failed to parse UDP packet from $sender: ${e.getMessage}"
                  )

          catch
            case _: InterruptedException =>
              Thread.currentThread().interrupt()
            case e: Exception =>
              if !s.isClosed then
                logger.error("Error receiving node status", e)
      ,
      "NodeStatus-Receiver"
    )

    t.setDaemon(true)
    t.start()
    thread = Some(t)

  def stop(): Unit =
    logger.info("Stopping NodeStatus receiver")
    thread.foreach(_.interrupt())
    socket.foreach(_.close())
    thread = None
    socket = None