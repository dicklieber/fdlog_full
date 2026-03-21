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

import java.net.URI
import scala.util.matching.Regex

/**
 * Represents the identity of a network node, encapsulating details such as host, port, and an instance ID.
 *
 * @constructor Creates a new NodeIdentity with a specified host, port, and instance ID.
 *              Default values for host and port are "44.0.0.1" and 42, respectively.
 * @param host       The hostname or IP address of the node.
 * @param port       The port number on which the node is reachable.
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
case class NodeIdentity(host: String = "local",
                        port: Int = 42,
						hostName:String,
                        instanceId: Id = "") extends Ordered[NodeIdentity]:
  override val toString: String =
    f"$host:$port_$instanceId"
	
  val hostAndPort: String = s"$host:$port"
  
  val udpHeaderPiece:String=
  	s"$port_${instanceId}_$hostName"
	
  lazy val short:String =
    host.split('.').last

  def toURL: String =
    toURI.toString

  def toURI: URI =
    new URI(
      "http", // scheme
      instanceId, // userInfo
      host,
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

  def fromURI(uri: URI): NodeIdentity =
    NodeIdentity(host = uri.getHost, port = uri.getPort, instanceId = uri.getUserInfo)

  private val regx = """^(localhost|[0-9.]+):(\d{1,5})-(.*)$""".r

  given Encoder[NodeIdentity] = Encoder.encodeString.contramap(_.toString)
  given Decoder[NodeIdentity] = Decoder.decodeString.map(NodeIdentity.apply)
  given KeyEncoder[NodeIdentity] = KeyEncoder.encodeKeyString.contramap(_.toString)
  given KeyDecoder[NodeIdentity] = KeyDecoder.instance(s => Some(NodeIdentity(s)))
  given Schema[NodeIdentity] = Schema.string

  def apply(sourceHost:String, udpPiece:String)=
  //todo
  def apply(s: String): NodeIdentity =
      s match
        case "local" => NodeIdentity()
        case regx(host, sPort, instanceId) =>
          NodeIdentity(host, sPort.toInt, instanceId)
        case _ =>
          // Try to parse just host:port for backward compatibility if needed, 
          // but based on toString it should always have -instanceId
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