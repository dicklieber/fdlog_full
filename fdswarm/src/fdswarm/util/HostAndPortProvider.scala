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

package fdswarm.util

import jakarta.inject.{Inject, Named, Singleton}


@Singleton
class HostAndPortProvider @Inject(@Named("fdswarm.httpPort") httpPort: Int):

  // Override port with PORT env var if present
  private val port = sys.env.get("PORT").map { sPort =>
    sPort.toInt
  }.getOrElse(httpPort)
  val http: HostAndPort = apply(port)

  private def apply(port: Int): HostAndPort = HostAndPort(localIPv4Address, port)

  private def localIPv4Address: String =
    import java.net.NetworkInterface
    import scala.jdk.CollectionConverters.*

    val interfaces = NetworkInterface.getNetworkInterfaces.asScala
    val addresses = for {
      interface <- interfaces
      if interface.isUp && !interface.isLoopback && !interface.isVirtual
      address <- interface.getInetAddresses.asScala
      if address.isSiteLocalAddress && address.getHostAddress.contains(".")
    } yield address.getHostAddress

    addresses.toSeq.headOption.getOrElse("127.0.0.1")


