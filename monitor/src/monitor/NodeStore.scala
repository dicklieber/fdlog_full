package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.NodeIdentity
import jakarta.inject.Singleton
import scalafx.collections.ObservableBuffer

import scala.collection.concurrent.TrieMap

/**
  * What the monitor knows.
  */
@Singleton
class NodeStore extends LazyStructuredLogging:
  private val _nodes = TrieMap[NodeIdentity, NodeData]()
  val observableNodes: ObservableBuffer[NodeData] = ObservableBuffer[NodeData]()

  def statusReceived(nodeIdentity: NodeIdentity): Unit = _nodes.get(nodeIdentity) match
    case Some(existingNodeData) => existingNodeData.updateLastStatus()
    case None                   =>
      val nodeData = NodeData(nodeIdentity)
      _nodes.put(nodeIdentity, nodeData)
      observableNodes += nodeData

  def updateNodeData(nodeIdentity: NodeIdentity, indexOperation: IndexOperation): Unit =
    val nodeData = _nodes(nodeIdentity) // throws if we have none, should never happen.
    nodeData.updateLastIndexOp(indexOperation)

  def nodes:Seq[NodeData] =
    _nodes.values.toSeq