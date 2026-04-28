package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.UDPHeaderData
import fdswarm.util.NodeIdentity
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane
import scalafx.stage.Window

import java.util.concurrent.LinkedBlockingQueue
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

@Singleton
class NodeInfoManager @Inject()(udpPacketListener:UdpPacketListener) extends LazyStructuredLogging:
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

  def showNodeIdentityDialog(ownerWindow: Window): Unit =
    val table = new TableView[NodeIdentity](nodeIdentities):
      columnResizePolicy = TableView.ConstrainedResizePolicy
      columns ++= Seq(
        new TableColumn[NodeIdentity, String]:
          text = "Host"
          cellValueFactory = c => StringProperty(c.value.hostName)
          prefWidth = 180
        ,
        new TableColumn[NodeIdentity, String]:
          text = "IP"
          cellValueFactory = c => StringProperty(c.value.hostIp)
          prefWidth = 150
        ,
        new TableColumn[NodeIdentity, Int]:
          text = "Port"
          cellValueFactory = c => ObjectProperty(c.value.port)
          prefWidth = 80
        ,
        new TableColumn[NodeIdentity, String]:
          text = "Instance"
          cellValueFactory = c => StringProperty(c.value.instanceId)
          prefWidth = 180
        ,
        new TableColumn[NodeIdentity, String]:
          text = "Node"
          cellValueFactory = c => StringProperty(c.value.toString)
          prefWidth = 320
      )

    val dialog = new Dialog[Unit]():
      initOwner(ownerWindow)
      title = "Node Identities"
      headerText = "Known UDP Node Identities"
      resizable = true

    dialog.dialogPane().content = new BorderPane:
      center = table
      padding = Insets(10)
      prefWidth = 800
      prefHeight = 450
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.showAndWait()

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
