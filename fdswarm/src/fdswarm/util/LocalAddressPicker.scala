package fdswarm.util

import java.net.*
import scala.jdk.CollectionConverters.*

/**
 * Utility object providing functionality to determine the best IP address of the current system.
 * The class contains methods to extract the most suitable IP address based on network routing
 * or scanning available network interfaces, with filtering and scoring applied to prioritize
 * private and preferred IP ranges.
 * This is, probably, way overkill, ChatGPT generated code!
 */
object LocalAddressPicker:

  def bestIp: String =
    // 1) Try routing-based detection (most reliable)
    routeBasedIp()
      .orElse(scanBasedIp())
      .getOrElse("127.0.0.1")

  // --- Method 1: Ask OS "what would you use to reach the world?"
  private def routeBasedIp(): Option[String] =
    val targets = List(
      "8.8.8.8",        // public internet (Google DNS)
      "1.1.1.1",        // Cloudflare
      "192.168.1.1"     // common LAN gateway (may or may not exist)
    )

    targets.view.flatMap { target =>
      try
        val socket = new DatagramSocket()
        socket.connect(InetAddress.getByName(target), 80)
        val addr = socket.getLocalAddress
        socket.close()

        if isUsable(addr) then Some(addr.getHostAddress)
        else None
      catch
        case _: Exception => None
    }.headOption

  // --- Method 2: Scan interfaces
  private def scanBasedIp(): Option[String] =
    NetworkInterface.getNetworkInterfaces.asScala.iterator
      .filter(iface => iface.isUp && !iface.isLoopback && !iface.isPointToPoint)
      .flatMap(_.getInetAddresses.asScala)
      .collect { case addr: Inet4Address if isUsable(addr) => addr }
      .toList
      .sortBy(score)
      .headOption
      .map(_.getHostAddress)

  // --- Filters
  private def isUsable(addr: InetAddress): Boolean =
    !addr.isLoopbackAddress &&
      !addr.isLinkLocalAddress

  // --- Preference scoring
  private def score(addr: Inet4Address): Int =
    val ip = addr.getHostAddress
    if ip.startsWith("192.168.") then 0
    else if ip.startsWith("10.") then 1
    else if isPrivate172(ip) then 2
    else 10

  private def isPrivate172(ip: String): Boolean =
    val p = ip.split("\\.")
    p.length == 4 &&
      p(0) == "172" &&
      p(1).toIntOption.exists(n => n >= 16 && n <= 31)