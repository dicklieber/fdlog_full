package fdswarm.tools

import java.net.*

@main def multicastDebug(): Unit =

  val groupAddr = "239.192.0.88"
  val port = 8900

  println("---- Interfaces ----")

  val ifaces = NetworkInterface.getNetworkInterfaces
  while ifaces.hasMoreElements do
    val ni = ifaces.nextElement()
    val addrs = ni.getInetAddresses

    while addrs.hasMoreElements do
      val addr = addrs.nextElement()
      println(s"${ni.getName}  ${addr.getHostAddress}")

  println()

  val group = InetAddress.getByName(groupAddr)

  val socket = new MulticastSocket(port)

  // choose interface that has your LAN address
  val iface =
    NetworkInterface.getByInetAddress(
      InetAddress.getByName("192.168.0.59")
    )

  println(s"Joining multicast group $groupAddr on interface ${iface.getName}")

  socket.joinGroup(
    new InetSocketAddress(group, port),
    iface
  )

//  val buf = Array.ofDim
  val buf = new Array[Byte](4024)
  println(s"Listening on port $port...")

  while true do
    val packet = DatagramPacket(buf, buf.length)
    socket.receive(packet)

    val msg = String(packet.getData, 0, packet.getLength)

    println(
      s"Received from ${packet.getAddress.getHostAddress}: $msg"
    )