package monitor

import fdswarm.util.NodeIdentity

import java.time.Instant

/**
  * Represents an operation related to indexing with metadata on item counts, offsets, and a
  * timestamp indicating when the operation was created.
  *
  * @param itemCount The number of items involved in the index operation.
  * @param offset The offset where this piece of the log ended.
  * @param stamp when.
  */
case class IndexOperation(itemCount: Int, offset: Long, stamp: Instant = Instant.now())

object IndexOperation:
  val Never: IndexOperation = IndexOperation(-1, 0, Instant.EPOCH)

/**
  * @param nodeIdentity which node.
  */
class NodeData(val nodeIdentity: NodeIdentity):
  // when we got a [[fdswarm.replication.StatusMessage]] fronm the node
  private var lastStatus: Instant = Instant.now()
  // what we did scraping the node log and pushing to ElasticSearch
  var lastIndexOp: IndexOperation = IndexOperation.Never
  def updateLastStatus(): Unit = lastStatus = Instant.now()
  def updateLastIndexOp(op: IndexOperation): Unit = lastIndexOp = op
