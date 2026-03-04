package fdswarm.replication

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.{HostAndPortProvider, Ids}
import jakarta.inject.{Inject, Singleton}

import java.net.{DatagramPacket, InetAddress, InetSocketAddress, MulticastSocket, NetworkInterface}
import scala.compiletime.uninitialized
import java.util.concurrent.LinkedBlockingQueue
import scala.jdk.CollectionConverters.*

@Singleton
class MulticastTransport @Inject()(
                                    @Named("fdswarm.UDP.port") port: Int,
                                    @Named("fdswarm.UDP.groupAddr") groupAddr: String,
                                    hostAndPortProvider: HostAndPortProvider
                                  ) extends LazyLogging:
  logger.debug("Starting MulticastTransport on {}:{}", groupAddr, port)
  val queue = new LinkedBlockingQueue[UDPHeaderData]()

  private val group = InetAddress.getByName(groupAddr)
  private val socketAddress = new InetSocketAddress(group, port)
  private var socket: MulticastSocket = uninitialized
  private var thread: Thread = uninitialized

  private def getNetworkInterface: NetworkInterface =
    val ip = hostAndPortProvider.currentIp.ip
    logger.debug(s"Using IP $ip for MulticastTransport")
    NetworkInterface.getByInetAddress(InetAddress.getByName(ip))

  // Start receiver in constructor
  try
    socket = new MulticastSocket(null)
    socket.setReuseAddress(true)
    socket.bind(new InetSocketAddress(port))

    val ni = getNetworkInterface
    socket.setNetworkInterface(ni)
    socket.joinGroup(socketAddress, ni)

    thread = new Thread(() =>
      val buffer = new Array[Byte](65535)

      while !Thread.currentThread().isInterrupted do
        try
          val packet = new DatagramPacket(buffer, buffer.length)
          socket.receive(packet)
          logger.trace(s"Received UDP packet from ${packet.getAddress}:${packet.getPort}")

          val data = packet.getData.slice(packet.getOffset, packet.getOffset + packet.getLength)
          try
            UDPHeader.parse(data) match
              case Some(udpHeader) =>
                logger.trace(s"Received UDP packet from ${packet.getAddress}:${packet.getPort}: ${udpHeader.service}")
                queue.offer(udpHeader)
              case None =>
                logger.debug(s"Ignoring our own message from ${packet.getAddress}:${packet.getPort}")
          catch
            case e: IllegalArgumentException =>
              logger.error(s"Received invalid UDP packet: ${e.getMessage}")
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()
          case e: Exception =>
            if socket != null && !socket.isClosed then
              logger.error(s"Error in MulticastTransport receiver: ${e.getMessage}")
    , "Multicast-Receiver")

    thread.setDaemon(true)
    thread.start()
  catch
    case e: Exception =>
      logger.error(s"Failed to start MulticastTransport receiver: ${e.getMessage}")
      stop()

  def send(data: Array[Byte]): Unit =
    send(Service.QSO, data)

  def send(service: Service, data: Array[Byte]): Unit =
    val packetBytes = UDPHeader(service, data)
    val packet = new DatagramPacket(packetBytes, packetBytes.length, group, port)
    socket.send(packet)

  def stop(): Unit =
    logger.debug("Stopping MulticastTransport")
    if thread != null then
      thread.interrupt()
      thread = null

    if socket != null then
      try
        if !socket.isClosed then
          val ni = getNetworkInterface
          socket.leaveGroup(socketAddress, ni)
          socket.close()
      catch
        case e: Exception => logger.debug(s"Error while closing MulticastSocket: ${e.getMessage}")
      finally
        socket = null