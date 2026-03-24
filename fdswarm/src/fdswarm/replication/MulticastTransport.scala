package fdswarm.replication

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.NodeIdentityManager
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

import java.net.*
import java.util.concurrent.LinkedBlockingQueue
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

@Singleton
class MulticastTransport @Inject() (
                                     @Named("fdswarm.UDP.port") port: Int,
                                     @Named("fdswarm.UDP.groupAddr") groupAddr: String,
                                     val nodeIdentityManager: NodeIdentityManager,
                                     meterRegistry: MeterRegistry
                                   ) extends Transport with LazyLogging:

  logger.debug("Starting MulticastTransport on {}:{}", groupAddr, port)

  override val mode: String = "Multicast"
  val queue = new LinkedBlockingQueue[UDPHeaderData]()
  private val listeners = new java.util.concurrent.CopyOnWriteArrayList[UDPHeaderData => Unit]()

  private val sendCounter = meterRegistry.counter("fdswarm_sent_packets_total", "mode", mode)
  private var lastPacketBytes: Int = 0
  meterRegistry.gauge("fdswarm_sent_packet_bytes", this, (mt: MulticastTransport) => mt.lastPacketBytes.toDouble)

  def addListener(listener: UDPHeaderData => Unit): Unit = listeners.add(listener)
  def removeListener(listener: UDPHeaderData => Unit): Unit = listeners.remove(listener)

  private val group = InetAddress.getByName(groupAddr)
  private val socketAddress = new InetSocketAddress(group, port)

  private var socket: MulticastSocket = uninitialized
  private var thread: Thread = uninitialized

  private def getNetworkInterfaceOrThrow(): NetworkInterface =
    val ip = nodeIdentityManager.currentIp.ip
    logger.debug(s"Using IP $ip for MulticastTransport")
    val ni = NetworkInterface.getByInetAddress(InetAddress.getByName(ip))
    if ni == null then
      val all = NetworkInterface.getNetworkInterfaces.asScala.toList
      val msg =
        s"Could not find NetworkInterface for ip=$ip. Available: " +
          all.map { n =>
            val addrs = n.getInetAddresses.asScala.mkString(",")
            s"${n.getName}(${n.getDisplayName}) up=${n.isUp} loop=${n.isLoopback} mc=${n.supportsMulticast} addrs=[$addrs]"
          }.mkString("; ")
      throw new RuntimeException(msg)
    ni

  private def maybeLoopbackInterface(exclude: NetworkInterface): Option[NetworkInterface] =
    NetworkInterface.getNetworkInterfaces.asScala.find { n =>
      try n.isLoopback && n.isUp && n.supportsMulticast && n != exclude
      catch case _: Exception => false
    }

  // Start receiver in constructor
  try
    socket = new MulticastSocket(null)

    // Important on some OSes: set before bind
    socket.setReuseAddress(true)

    // Bind explicitly to wildcard (often helps Windows/VM multicast reception)
    socket.bind(new InetSocketAddress("0.0.0.0", port))

    val ni = getNetworkInterfaceOrThrow()

    logger.debug(s"Setting network interface to ${ni.getName} (${ni.getDisplayName})")
    socket.setNetworkInterface(ni)

    // Allow same-host multicast delivery (macOS often needs this for 2 local processes)
    // NOTE: API is inverted: true = disable loopback, false = enable loopback
    socket.setLoopbackMode(false)

    // TTL: keep it >1 to avoid surprises with some local network setups (still stays on LAN)
    socket.setTimeToLive(8)

    // Join on ALL multicast-capable interfaces (avoids "picked the wrong NIC" problems on macOS)
    val ifaces =
      NetworkInterface.getNetworkInterfaces.asScala.toList
        .filter { n =>
          try
            n.isUp &&
            n.supportsMulticast &&
            n.getInetAddresses.asScala.exists(_.isInstanceOf[java.net.Inet4Address])
          catch case _: Exception => false
        }

    ifaces.foreach { n =>
      try
        val addrs = n.getInetAddresses.asScala.mkString(", ")
        socket.joinGroup(socketAddress, n)
        logger.info(s"Joined multicast $groupAddr:$port on ${n.getName} (${n.getDisplayName}) addrs=[$addrs]")
      catch
        case e: Exception =>
          logger.debug(s"Join failed on ${n.getName} (${n.getDisplayName}): ${e.getMessage}")
    }

    thread = new Thread(
      () =>
        val buffer = new Array[Byte](65535)

        while !Thread.currentThread().isInterrupted do
          try
            val packet: DatagramPacket = new DatagramPacket(buffer, buffer.length)
            socket.receive(packet)
            val senderAddr = packet.getAddress
            val senderPort = packet.getPort
            logger.info(s"Received UDP packet from $senderAddr:$senderPort, length=${packet.getLength}")

            try
              val udpHeader = UDPHeader.parse(packet)
              logger.info(s"Parsed UDP: service=${udpHeader.service} node=${udpHeader.nodeIdentity} pktSender=$senderAddr:$senderPort")
              if nodeIdentityManager.isUs(udpHeader.nodeIdentity) then
                logger.trace(s"Ignoring our own loopback message")
              else
                listeners.forEach(_.apply(udpHeader))
                queue.offer(udpHeader)
            catch
              case e: IllegalArgumentException =>
                logger.error(
                  s"Received invalid UDP packet from $senderAddr:$senderPort: ${e.getMessage}"
                )
      ,
      "Multicast-Receiver"
    )

    thread.setDaemon(true)
    thread.start()

  catch
    case e: Exception =>
      logger.error(
        s"Failed to start MulticastTransport receiver: ${e.getMessage}",
        e
      )
      stop()

  private val sentCounter = new java.util.concurrent.atomic.LongAdder()
  override def sentCount: Long = sentCounter.sum()

  def send(data: Array[Byte]): Unit =
    send(Service.QSO, data)

  def send(service: Service, data: Array[Byte]): Unit =
    val packetBytes = UDPHeader(service, nodeIdentityManager.ourNodeIdentity, data)
    lastPacketBytes = packetBytes.length
    val packet =
      new DatagramPacket(packetBytes, packetBytes.length, group, port)
    socket.send(packet)
    sentCounter.increment()
    sendCounter.increment()

  def stop(): Unit =
    logger.debug("Stopping MulticastTransport")

    // Close the socket first to unblock receive()
    if socket != null then
      try
        if !socket.isClosed then socket.close()
      catch
        case e: Exception =>
          logger.debug(s"Error while closing MulticastSocket: ${e.getMessage}")
      finally socket = null

    if thread != null then
      thread.interrupt()
      thread = null