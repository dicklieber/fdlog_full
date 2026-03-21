/*
 * Copyright (C) 2022 Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fdswarm.util

import fdswarm.util.Ids.Id
import io.circe.*
import sttp.tapir.Schema

import java.net.{InetAddress, URI}
import scala.util.matching.Regex

/**
 * Represents the identity of a network node, encapsulating details such as host, port, and an instance ID.
 *
 * @param hostIp     The hostname or IP address of the node. This will default to "local".
 *                   When received host will be replaced with the source of the UDP message.
 *
 * @param port       The port number on which the node is reachable.
 * @param hostName   The hostname of the node.
 * @param instanceId A unique identifier for the instance of the node.
 *
 *                   Extends the `Ordered` trait to allow comparison of `NodeIdentity` instances based on host and port.
 *
 *                   Methods:
 *                   - `toString`: Returns a string representation of the node in the format `host:port-instanceId`.
 *                   - `short`: A lazily evaluated property that provides the last segment of the host name.
 *                   - `toURL`: Converts the node's information into a URL string.
 *                   - `toURI`: Converts the node's information into a URI instance using the scheme "http".
 *                   - `compare`: Compares two `NodeIdentity` instances first by host, then by port.
 */
case class NodeIdentity(hostIp: String = "local", port: Int = 42, hostName: String, instanceId: Id = "") extends Ordered[NodeIdentity]:
  lazy val short: String =
    hostIp.split('.').last
  /**
   * String representation.
   * This is the complement to the [[NodeIdentity.apply(s:String)]] method.
   */
  override val toString: String =
    f"${hostIp}_${port}_${instanceId}_$hostName"
  /**
   * UDP header piece is used to identify the node.
   * This gets put into the [[fdswarm.replication.UDPHeader]].
   */
  val udpHeaderPiece: String =
    s"${port}_${instanceId}_$hostName"

  def toURL: String =
    toURI.toString

  def toURI: URI =
    new URI(
      "http", // scheme
      instanceId, // userInfo
      hostIp,
      port,
      null, // path
      null, // query
      null // fragment
    )

  /**
   * Compares this NodeIdentity with another NodeIdentity based on instanceId..
   * Order is arbitrary, but consistent.
   *
   */
  override def compare(that: NodeIdentity): Int =
    this.instanceId.compareTo(that.instanceId)

object NodeIdentity:
  val testNodeIdentity = NodeIdentity("44.0.0.1", 42, "testHost", "=id")
  private val regx = """([^:]+)_(\d+)_(.{3})_(.*)""".r
  private val regxUdp = """(\d+)_(.{3})_(.*)""".r

  def apply(port: Int, hostName: String): NodeIdentity =
    NodeIdentity(hostName, port, "")

  given Encoder[NodeIdentity] = Encoder.encodeString.contramap(_.toString)
  given Decoder[NodeIdentity] = Decoder.decodeString.map(NodeIdentity.apply)
  given KeyEncoder[NodeIdentity] = KeyEncoder.encodeKeyString.contramap(_.toString)
  given KeyDecoder[NodeIdentity] = KeyDecoder.instance(s => Some(NodeIdentity(s)))
  given Schema[NodeIdentity] = Schema.string

  /**
   *
   * @param address  from packet.getAddress as received from UDP.
   * @param udpPiece from the [[udpPiece]].
   */
  def fromUdpHeader(address: InetAddress, udpPiece: String) =
    udpPiece match
      case regxUdp(port, hostName, instanceId) =>
        NodeIdentity(hostIp = address.getHostAddress,
          port = port.toInt,
          hostName = instanceId,
          instanceId = hostName)
      case _ =>
        throw new IllegalArgumentException(s"Invalid UdpPiece in header: $udpPiece")

/**
 *
 * @param s from [[toString]]
 * @return
 */
  def apply(s: String): NodeIdentity =
      s match
        case regx(hostIp, sPort, hostName, instanceId) =>
          NodeIdentity(hostIp = hostIp, port = sPort.toInt, hostName = instanceId, instanceId = hostName)
        case _ =>
          throw new IllegalArgumentException(s"Invalid NodeIdentity: $s")

import io.circe.{Decoder, Encoder}

/**
 * Represents a combination of a numeric port and an instance identifier.
 *
 * @param port       The numeric port associated with the instance.
 * @param instanceId The unique identifier for the instance.
 *
 *                   The `toString` method provides a string representation
 *                   in the format `port-instanceId`.
 */
case class PortAndInstance(port: Int, instanceId: Id):
  override def toString: String =
    s"$port-$instanceId"

object PortAndInstance:
  private val Pattern: Regex = """^(\d+)-(.+)$""".r

  /** Parse and throw if invalid (useful when input is trusted). */
  def fromString(s: String): PortAndInstance =
    parse(s).fold(err => throw new IllegalArgumentException(err), identity)


  def parse(s: String): Either[String, PortAndInstance] =
    s match
      case Pattern(portStr, idStr) =>
        portStr.toIntOption match
          case Some(port) =>
            val portAndInstance = PortAndInstance(port, idStr)
            Right(portAndInstance)
          case None       => Left(s"Invalid port: $portStr")
      case _ =>
        Left(s"Invalid PortAndInstance format: $s")

  given Encoder[PortAndInstance] =
    Encoder.encodeString.contramap(_.toString)

  given Decoder[PortAndInstance] =
    Decoder.decodeString.emap(s => parse(s))