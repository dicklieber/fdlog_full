package fdswarm.replication

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import jakarta.inject.Inject

import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


class BroadcastSender @Inject() (config: Config) extends LazyLogging:

  private val broadcastPort = if config.hasPath("fdswarm.broadcastPort") then config.getInt("fdswarm.broadcastPort") else 2234
  private val broadcastAddress = if config.hasPath("fdswarm.broadcastAddress") then config.getString("fdswarm.broadcastAddress") else "255.255.255.255"

  def broadcast(str: String): Unit =
    val socket = new java.net.DatagramSocket()
    try
      socket.setBroadcast(true)
      val bytes = str.getBytes("UTF-8")
      val packet = new java.net.DatagramPacket(
        bytes,
        bytes.length,
        java.net.InetAddress.getByName(broadcastAddress),
        broadcastPort
      )
      socket.send(packet)
      logger.debug(s"Broadcasted ${bytes.length} bytes to $broadcastAddress:$broadcastPort")
    catch
      case e: Exception =>
        logger.error(s"Failed to broadcast to $broadcastAddress:$broadcastPort", e)
    finally
      socket.close()

