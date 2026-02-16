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

import jakarta.inject.*
import upickle.default.{ReadWriter, readwriter}

import java.net.{InetAddress, InetSocketAddress}

case class HostAndPort(host: String, port: Int) extends Ordered[HostAndPort]:
  override val toString: String =
    f"$host:$port%d"

  def withPort(port: Int): HostAndPort = copy(port = port)

  def toSocketAddress: InetSocketAddress =
    new InetSocketAddress(toInetAddress, port)

  private def toInetAddress: InetAddress = InetAddress.getByName(host)


  override def compare(that: HostAndPort): Int =
    var ret = this.host.compareTo(that.host)
    if (ret == 0)
      ret = this.port.compareTo(that.port)
    ret

object HostAndPort:
  private val regx = """^(localhost|[0-9.]+):(\d{1,5})$""".r

  given ReadWriter[HostAndPort] = readwriter[String]
    .bimap[HostAndPort](
      hostAndPort => hostAndPort.toString,
      HostAndPort.apply)

  def apply(s: String): HostAndPort =
    s match
      case regx(host, sPort) =>
        HostAndPort(host, sPort.toInt)
      case _ =>
        throw new IllegalArgumentException(s"Invalid host and port: $s")
    