package monitor

import fdswarm.logging.LazyStructuredLogging
import jakarta.inject.*

import java.util.concurrent.LinkedBlockingQueue

@Singleton
class NodeInfoManager @Inject()(udpPacketListener:UdpPacketListener) extends LazyStructuredLogging:
  private val queue: LinkedBlockingQueue[NodeInfo] = udpPacketListener.incomingQueue
  @volatile private var stopped = false
