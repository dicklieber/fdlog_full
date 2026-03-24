package fdswarm.replication

import java.util.concurrent.{BlockingQueue, TimeUnit}
import java.util.{Collection, Iterator => JIterator}

/**
 * A queue that can be invalidated (becomes "dead").
 * Once dead, it stops accepting elements and unblocks callers.
 */
class LiveOrDeadQueue(initial: BlockingQueue[UDPHeaderData])
  extends BlockingQueue[UDPHeaderData]:

  @volatile private var queue: BlockingQueue[UDPHeaderData] = initial

  def invalidateQueue(): Unit =
    queue = DeadQueue

  def isDead: Boolean =
    queue eq DeadQueue

  override def put(e: UDPHeaderData): Unit =
    checkAlive()
    queue.put(e)

  override def take(): UDPHeaderData =
    if isDead then
      throw IllegalStateException("Queue is dead")
    queue.take()

  override def offer(
                      e: UDPHeaderData,
                      timeout: Long,
                      unit: TimeUnit
                    ): Boolean =
    checkAlive()
    queue.offer(e, timeout, unit)

  override def poll(timeout: Long, unit: TimeUnit): UDPHeaderData =
    if isDead then null
    else queue.poll(timeout, unit)

  override def remainingCapacity(): Int =
    if isDead then 0 else queue.remainingCapacity()

  override def drainTo(c: Collection[_ >: UDPHeaderData]): Int =
    queue.drainTo(c)

  override def drainTo(
                        c: Collection[_ >: UDPHeaderData],
                        maxElements: Int
                      ): Int =
    queue.drainTo(c, maxElements)

  override def offer(e: UDPHeaderData): Boolean =
    if isDead then false else queue.offer(e)

  override def poll(): UDPHeaderData =
    if isDead then null else queue.poll()

  override def peek(): UDPHeaderData =
    if isDead then null else queue.peek()

  override def iterator(): JIterator[UDPHeaderData] =
    queue.iterator()

  override def size(): Int =
    queue.size()

  override def add(e: UDPHeaderData): Boolean =
    if isDead then throw IllegalStateException("Queue is dead")
    else queue.add(e)

  override def remove(): UDPHeaderData =
    val value = poll()
    if value == null then throw java.util.NoSuchElementException()
    value

  override def element(): UDPHeaderData =
    val value = peek()
    if value == null then throw java.util.NoSuchElementException()
    value

  override def contains(o: Any): Boolean =
    queue.contains(o)

  override def toArray(): Array[Object] =
    queue.toArray()

  override def toArray[T <: Object](a: Array[T]): Array[T] =
    queue.toArray(a.asInstanceOf[Array[Object]]).asInstanceOf[Array[T]]

  override def remove(o: Any): Boolean =
    queue.remove(o)

  override def containsAll(c: Collection[?]): Boolean =
    queue.containsAll(c)

  override def addAll(c: Collection[_ <: UDPHeaderData]): Boolean =
    checkAlive()
    queue.addAll(c)

  override def removeAll(c: Collection[?]): Boolean =
    queue.removeAll(c)

  override def retainAll(c: Collection[?]): Boolean =
    queue.retainAll(c)

  override def clear(): Unit =
    queue.clear()

  override def isEmpty(): Boolean =
    queue.isEmpty()

  private def checkAlive(): Unit =
    if isDead then
      throw IllegalStateException("Queue is dead")

object DeadQueue extends BlockingQueue[UDPHeaderData]:

  override def put(e: UDPHeaderData): Unit =
    throw IllegalStateException("Queue is dead")

  override def take(): UDPHeaderData =
    throw IllegalStateException("Queue is dead")

  override def offer(e: UDPHeaderData): Boolean =
    false

  override def offer(
                      e: UDPHeaderData,
                      timeout: Long,
                      unit: TimeUnit
                    ): Boolean =
    false

  override def poll(): UDPHeaderData =
    null

  override def poll(timeout: Long, unit: TimeUnit): UDPHeaderData =
    null

  override def peek(): UDPHeaderData =
    null

  override def remainingCapacity(): Int =
    0

  override def drainTo(c: Collection[_ >: UDPHeaderData]): Int =
    0

  override def drainTo(
                        c: Collection[_ >: UDPHeaderData],
                        maxElements: Int
                      ): Int =
    0

  override def iterator(): JIterator[UDPHeaderData] =
    java.util.Collections.emptyIterator()

  override def size(): Int =
    0

  override def add(e: UDPHeaderData): Boolean =
    throw IllegalStateException("Queue is dead")

  override def remove(): UDPHeaderData =
    throw java.util.NoSuchElementException()

  override def element(): UDPHeaderData =
    throw java.util.NoSuchElementException()

  override def contains(o: Any): Boolean =
    false

  override def toArray(): Array[Object] =
    Array.empty[Object]

  override def toArray[T <: Object](a: Array[T]): Array[T] =
    a

  override def remove(o: Any): Boolean =
    false

  override def containsAll(c: Collection[?]): Boolean =
    c.isEmpty

  override def addAll(c: Collection[_ <: UDPHeaderData]): Boolean =
    if c.isEmpty then false
    else throw IllegalStateException("Queue is dead")

  override def removeAll(c: Collection[?]): Boolean =
    false

  override def retainAll(c: Collection[?]): Boolean =
    false

  override def clear(): Unit =
    ()

  override def isEmpty(): Boolean =
    true