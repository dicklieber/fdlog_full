package monitor

import fdswarm.util.NodeIdentity
import scalafx.beans.property.{IntegerProperty, LongProperty, ObjectProperty}

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
class NodeData(val nodeIdentity: NodeIdentity, initialLastIndexOffset: Long = IndexOperation.Never.offset):
  // when we got a [[fdswarm.replication.StatusMessage]] fronm the node
  val lastStatus = ObjectProperty[Instant](this, "lastStatus", Instant.now())
  // what we did scraping the node log and pushing to ElasticSearch
  val lastIndexItemCount = new IntegerProperty(this, "lastIndexItemCount", IndexOperation.Never.itemCount)
  val lastIndexOffset = new LongProperty(this, "lastIndexOffset", initialLastIndexOffset)
  val lastIndexStamp = ObjectProperty[Instant](this, "lastIndexStamp", IndexOperation.Never.stamp)

  def updateLastStatus(): Unit = lastStatus.value = Instant.now()
  def updateLastIndexOp(op: IndexOperation): Unit =
    lastIndexItemCount.value = op.itemCount
    lastIndexOffset.value = op.offset
    lastIndexStamp.value = op.stamp
