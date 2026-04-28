package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.UDPHeaderData
import jakarta.inject.*

import java.util.concurrent.LinkedBlockingQueue

@Singleton
class NodeInfoManager @Inject()(udpPacketListener:UdpPacketListener) extends LazyStructuredLogging:
  private val queue: LinkedBlockingQueue[UDPHeaderData] = udpPacketListener.incomingQueue
  @volatile private var stopped = false
