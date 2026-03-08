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
import fdswarm.util.PortAndInstance.ourInstanceId
import io.circe.*
import io.circe.KeyEncoder.encodeKeyLong
import jakarta.inject.*
import sttp.tapir.Schema

import java.net.{InetAddress, InetSocketAddress, URI}
import java.util.Base64
import scala.util.matching.Regex

case class NodeIdentity(host: String = "44.0.0.1", port: Int = 42, instanceId: Id = "") extends Ordered[NodeIdentity]:
  override val toString: String =
    f"$host:$port%d-$instanceId"
  lazy val short:String =
    host.split('.').last
  def notUs: Boolean =
    instanceId != ourInstanceId

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

  override def compare(that: NodeIdentity): Int =
    var ret = this.host.compareTo(that.host)
    if (ret == 0)
      ret = this.port.compareTo(that.port)
    ret

object NodeIdentity:

  def fromURI(uri: URI): NodeIdentity =
    NodeIdentity(host = uri.getHost, port = uri.getPort, instanceId = uri.getUserInfo)

  private val regx = """^(localhost|[0-9.]+):(\d{1,5})-(.*)$""".r

  given Encoder[NodeIdentity] = Encoder.encodeString.contramap(_.toString)
  given Decoder[NodeIdentity] = Decoder.decodeString.map(NodeIdentity.apply)
  given KeyEncoder[NodeIdentity] = KeyEncoder.encodeKeyString.contramap(_.toString)
  given KeyDecoder[NodeIdentity] = KeyDecoder.instance(s => Some(NodeIdentity(s)))
  given Schema[NodeIdentity] = Schema.string

  def apply(s: String): NodeIdentity =
      s match
        case "local" => NodeIdentity()
        case regx(host, sPort, instanceId) =>
          NodeIdentity(host, sPort.toInt, instanceId)
        case _ =>
          // Try to parse just host:port for backward compatibility if needed, 
          // but based on toString it should always have -instanceId
          throw new IllegalArgumentException(s"Invalid NodeIdentity: $s")

import io.circe.{Encoder, Decoder}

/**
 * Represents a combination of a numeric port and an instance identifier.
 *
 * @param port       The numeric port associated with the instance.
 * @param instanceId The unique identifier for the instance.
 *
 *                   The `toString` method provides a string representation
 *                   in the format `port-instanceId`.
 */
case class PortAndInstance(port: Int, instanceId: Id = ourInstanceId):
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

  private var _ourInstanceId: String = ""

  def initOurInstanceId(directoryProvider: fdswarm.io.DirectoryProvider): Unit =
    val dir = directoryProvider()
    val file = dir / ".instanceId"
    if os.exists(file) then
      _ourInstanceId = os.read(file).trim
    else
      _ourInstanceId = Ids.generateInstanceId()
      os.makeDir.all(dir)
      os.write.over(file, _ourInstanceId)

  def ourInstanceId: String = _ourInstanceId