package monitor

import com.typesafe.config.Config
import fdswarm.logging.LazyStructuredLogging
import jakarta.inject.*

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.util.control.NonFatal

/**
  * Manages and updates information about UDP node identities and their packets.
  *
  * This class listens for incoming UDP packets, processes and manages information about the nodes
  * from which the packets originate, and provides the ability to scrape logs for discovered nodes.
  * It maintains an up-to-date list of node identities and their latest UDP header data.
  *
  * @constructor Creates a new instance of NodeInfoManager with dependencies injected.
  * @param udpPacketListener An instance of UdpPacketListener to receive and queue UDP packets.
  * @param nodeIdentityDialog A dialog interface for displaying node identities in a UI.
  * @param nodeLogScraper A scraper used for retrieving logs from discovered nodes.
  * @param config Configuration settings to adjust behavior such as log scraping interval.
  */
@Singleton
class LogProcesser @Inject() (nodeStore: NodeStore, nodeLogScraper: NodeLogScraper, config: Config)
    extends LazyStructuredLogging:
  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  scheduler.scheduleWithFixedDelay(() => scrapeDiscoveredNodeLogs(), 0, 10, TimeUnit.SECONDS)

  private def scrapeDiscoveredNodeLogs(): Unit =
    for
      nodeData <- nodeStore.nodes
    do
      try
        nodeLogScraper
          .scrapeNode(nodeData.nodeIdentity, nodeData.lastIndexOp.offset)
          .fold(
            e =>
              logger.error(s"Error scraping discovered $nodeData", e),
            indexOperation =>
              nodeStore.updateNodeData(nodeData.nodeIdentity, indexOperation)
          )
      catch case NonFatal(e) => logger.error(s"Error scraping discovered $nodeData", e)
