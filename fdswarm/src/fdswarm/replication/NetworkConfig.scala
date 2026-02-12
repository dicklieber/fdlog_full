/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
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
 *
 */

package fdswarm.replication

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config

import java.net.{Inet4Address, NetworkInterface, URI, URL}
import scala.jdk.CollectionConverters.*

@Singleton
class NetworkConfig @Inject() (config: Config):
  private val port:Int = sys.env.get("PORT")
    .map(_.toInt)
    .getOrElse(config.getInt("fdswarm.httpPort"))
  val url: URL =
    val address = findNonLocalhostIPv4().getOrElse("127.0.0.1")
    URI.create(s"http://$address:$port").toURL()

  private def findNonLocalhostIPv4(): Option[String] =
    NetworkInterface.getNetworkInterfaces.asScala
      .filter(ni => ni.isUp && !ni.isLoopback)
      .flatMap(_.getInetAddresses.asScala)
      .collectFirst {
        case addr: Inet4Address => addr.getHostAddress
      }
