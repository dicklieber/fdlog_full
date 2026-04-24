package fdswarm.replication

import fdswarm.util.{NodeIdentity, NodeIdentityManager}
import io.circe.Encoder
import io.circe.syntax.EncoderOps

import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue

trait Transport:
  val nodeIdentityManager: NodeIdentityManager
  val mode: String
  val incomingQueue: LinkedBlockingQueue[UDPHeaderData] = new LinkedBlockingQueue[UDPHeaderData]()

  def isUs(candidate: NodeIdentity): Boolean = nodeIdentityManager.isUs(candidate)

  /** Send a message of type [[Service]] with a given payload.
    */
  def send(service: Service[?], data: Array[Byte]): Unit

  def send[T: Encoder](service: Service[?], value: T): Unit =
    send(service, value.asJson.spaces2.getBytes(StandardCharsets.UTF_8))

  def stop(): Unit
