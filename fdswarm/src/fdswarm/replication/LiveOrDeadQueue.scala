package fdswarm.replication

import fdswarm.replication.LiveOrDeadQueue.deadQueueInstance
import fdswarm.replication.UDPHeaderData

import java.time.Instant
import java.util
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters.*

/**
 * A concurrent queue implementation that can be marked as either "live" or "dead."
 * When marked as "live," the queue supports normal operations such as adding,
 * retrieving, and draining elements. When marked as "dead," the queue becomes
 * non-operational and throws an exception upon any access attempt.
 *
 * The primary use case for this class is scenarios where the queue's availability
 * or operational state might change dynamically.
 */
final class LiveOrDeadQueue(val service:Service, val started:Instant = Instant.now):

  @volatile
  private var delegate: LinkedBlockingQueue[UDPHeaderData] =
    new LinkedBlockingQueue[UDPHeaderData]()

  def invalidateQueue(): Unit =
    delegate = deadQueueInstance

  def isAlive: Boolean =
    delegate ne deadQueueInstance

  def put(e: UDPHeaderData): Unit =
    delegate.put(e)

  def offer(e: UDPHeaderData): Boolean =
    delegate.offer(e)

  def offer(e: UDPHeaderData, timeout: Long, unit: TimeUnit): Boolean =
    delegate.offer(e, timeout, unit)

  def take(): UDPHeaderData =
    delegate.take()

  def poll(): UDPHeaderData | Null =
    delegate.poll()

  def poll(timeout: Long, unit: TimeUnit): UDPHeaderData | Null =
    delegate.poll(timeout, unit)

  def peek(): UDPHeaderData | Null =
    delegate.peek()

  def size: Int =
    delegate.size()

  def isEmpty: Boolean =
    delegate.isEmpty

  def drain(): List[UDPHeaderData] =
    val buf = new util.ArrayList[UDPHeaderData]()
    delegate.drainTo(buf)
    buf.asScala.toList

  def drain(max: Int): List[UDPHeaderData] =
    val buf = new util.ArrayList[UDPHeaderData]()
    delegate.drainTo(buf, max)
    buf.asScala.toList


object LiveOrDeadQueue:

  private class DeadQueue extends LinkedBlockingQueue[UDPHeaderData]:
    private def dead(): Nothing =
      throw new IllegalStateException("Queue is dead")

    override def put(e: UDPHeaderData): Unit = dead()
    override def offer(e: UDPHeaderData): Boolean = dead()
    override def offer(e: UDPHeaderData, timeout: Long, unit: TimeUnit): Boolean = dead()
    override def add(e: UDPHeaderData): Boolean = dead()
    override def take(): UDPHeaderData = dead()
    override def poll(): UDPHeaderData | Null = dead()
    override def poll(timeout: Long, unit: TimeUnit): UDPHeaderData | Null = dead()
    override def peek(): UDPHeaderData | Null = dead()
    override def clear(): Unit = dead()
    override def size(): Int = 0
    override def isEmpty: Boolean = dead()
    override def remainingCapacity(): Int = dead()
    override def contains(o: Any): Boolean = dead()
    override def remove(): UDPHeaderData = dead()
    override def remove(o: Any): Boolean = dead()
    override def element(): UDPHeaderData = dead()
    override def iterator(): util.Iterator[UDPHeaderData] = dead()
    override def toArray: Array[Object] = dead()
//    override def toArray[T](a: Array[T & Object]): Array[T & Object] = dead()
    override def drainTo(c: util.Collection[? >: UDPHeaderData]): Int = dead()
    override def drainTo(c: util.Collection[? >: UDPHeaderData], maxElements: Int): Int = dead()

  private val deadQueueInstance: LinkedBlockingQueue[UDPHeaderData] =
    new DeadQueue

