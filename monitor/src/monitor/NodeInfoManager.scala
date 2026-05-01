package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.UDPHeaderData
import fdswarm.util.NodeIdentity
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.stage.Window

import java.util.concurrent.LinkedBlockingQueue
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

@Singleton
class NodeInfoManager @Inject()(
    udpPacketListener: UdpPacketListener,
    nodeIdentityDialog: NodeIdentityDialog,
    logIndexer: ElasticsearchLogIndexer
) extends LazyStructuredLogging:
  private val queue: LinkedBlockingQueue[UDPHeaderData] = udpPacketListener.incomingQueue
  val latestHeaders: TrieMap[NodeIdentity, UDPHeaderData] = TrieMap.empty[NodeIdentity, UDPHeaderData]
  val nodeIdentities: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]
  private val thread = new Thread(
    () => consumePackets(),
    "Monitor-Node-Info-Manager"
  )
  @volatile private var stopped = false

  thread.setDaemon(true)
  thread.start()

  def stop(): Unit =
    stopped = true
    thread.interrupt()
    logIndexer.close()

  def showNodeIdentityDialog(ownerWindow: Window): Unit =
    nodeIdentityDialog.show(ownerWindow, nodeIdentities)

  private def consumePackets(): Unit =
    while !stopped && !Thread.currentThread().isInterrupted do
      try
        val udpHeaderData = queue.take()
        latestHeaders.put(udpHeaderData.nodeIdentity, udpHeaderData)
        updateNodeIdentityBuffer()
        logger.info(udpHeaderData.toString)
      catch
        case _: InterruptedException =>
          Thread.currentThread().interrupt()
        case NonFatal(e) =>
          logger.error("Error updating node info", e)

  private def updateNodeIdentityBuffer(): Unit =
    val nodes = latestHeaders.keys.toSeq.sorted
    updateOnFxThread {
      nodeIdentities.setAll(nodes*)
    }

  private def updateOnFxThread(action: => Unit): Unit =
    if Platform.isFxApplicationThread then action
    else
      try Platform.runLater(() => action)
      catch case _: IllegalStateException => action
