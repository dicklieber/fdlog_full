package monitor

import com.typesafe.config.Config
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.UDPHeaderData
import fdswarm.util.NodeIdentity
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.stage.Window

import java.time.Duration
import java.util.concurrent.{Executors, LinkedBlockingQueue, ScheduledExecutorService, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

@Singleton
class NodeInfoManager @Inject()(
    udpPacketListener: UdpPacketListener,
    nodeIdentityDialog: NodeIdentityDialog,
    nodeLogScraper: NodeLogScraper,
    config: Config
) extends LazyStructuredLogging:
  private val queue: LinkedBlockingQueue[UDPHeaderData] = udpPacketListener.incomingQueue
  val latestHeaders: TrieMap[NodeIdentity, UDPHeaderData] = TrieMap.empty[NodeIdentity, UDPHeaderData]
  val nodeIdentities: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]
  private val scrapeLogsEvery: Duration =
    if config.hasPath("monitor.scrapeLogs") then config.getDuration("monitor.scrapeLogs")
    else Duration.ofSeconds(15)
  private val scrapeLogsEveryMillis = Math.max(1L, scrapeLogsEvery.toMillis)
  private val scraperScheduler: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor((r: Runnable) =>
      val thread = new Thread(r, "Monitor-Node-Log-Scraper")
      thread.setDaemon(true)
      thread
    )
  private val thread = new Thread(
    () => consumePackets(),
    "Monitor-Node-Info-Manager"
  )
  @volatile private var stopped = false

  thread.setDaemon(true)
  thread.start()
  scraperScheduler.scheduleWithFixedDelay(
    () => scrapeDiscoveredNodeLogs(),
    scrapeLogsEveryMillis,
    scrapeLogsEveryMillis,
    TimeUnit.MILLISECONDS
  )

  def stop(): Unit =
    stopped = true
    thread.interrupt()
    scraperScheduler.shutdownNow()
    nodeLogScraper.close()

  def showNodeIdentityDialog(ownerWindow: Window): Unit =
    nodeIdentityDialog.show(ownerWindow, nodeIdentities)

  def nodeIdentityContent: scalafx.scene.layout.BorderPane =
    nodeIdentityDialog.content(nodeIdentities)

  private def consumePackets(): Unit =
    while !stopped && !Thread.currentThread().isInterrupted do
      try
        val udpHeaderData = queue.take()
        latestHeaders.put(udpHeaderData.nodeIdentity, udpHeaderData)
        updateNodeIdentityBuffer()
        logger.trace(udpHeaderData.toString)
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

  private def scrapeDiscoveredNodeLogs(): Unit =
    try
      val nodes = latestHeaders.keys.toSeq
      if nodes.nonEmpty then nodeLogScraper.scrapeNodes(nodes)
    catch
      case NonFatal(e) =>
        logger.error("Error scraping discovered node logs", e)
