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

import java.net.InetAddress

/**
 * Represents the identity of a network node, encapsulating details such as hostip, hostName, port, and an instance ID.
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
case class NodeIdentity(hostIp: String,
                        port: Int,
                        hostName: String,
                        instanceId: Id)
  extends Ordered[NodeIdentity] derives Codec.AsObject, Schema:
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

  val short: String = s"$hostName:$port"
  override def compare(that: NodeIdentity): Int =
    this.hostName.compareTo(that.hostName)

  override def equals(that: Any): Boolean = that match
    case other: NodeIdentity => this.instanceId == other.instanceId
    case _ => false

  override def hashCode: Int = instanceId.hashCode

object NodeIdentity:
  val testNodeIdentity = NodeIdentity(
    hostIp = "44.0.0.1",
    port = 42,
    hostName = "testHost",
    instanceId = "=id")
  private val regx = """([^:]+)_(\d+)_(.{3})_(.*)""".r
  private val regxUdp = """(\d+)_(.{3})_(.*)""".r

  /**
   *
   * @param address  from packet.getAddress as received from UDP.
   * @param udpPiece from the [[udpPiece]].
   */
  def fromUdpHeader(address: InetAddress, udpPiece: String): NodeIdentity =
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



