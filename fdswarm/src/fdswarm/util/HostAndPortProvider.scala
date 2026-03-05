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

import com.typesafe.scalalogging.LazyLogging
import jakarta.inject.{Inject, Named, Singleton}

import java.net.{Inet4Address, NetworkInterface}
import scala.jdk.CollectionConverters.*


@Singleton
class HostAndPortProvider @Inject(@Named("fdswarm.httpPort") httpPort: Int) extends LazyLogging:
  
  def suitableInterfaces: Seq[AnIpAddress] = (for
    interface: NetworkInterface <- NetworkInterface.getNetworkInterfaces.asScala
    anIpo = AnIpAddress(interface)
    if anIpo.hasIp
  yield
    logger.trace(s"anIpo: $anIpo")
    anIpo).toSeq

  private var ourIp:AnIpAddress = suitableInterfaces.headOption.getOrElse(AnIpAddress("loopback", "127.0.0.1"))

  def currentIp: AnIpAddress = ourIp
  def setIp(newIp: AnIpAddress): Unit =
    ourIp = newIp
    logger.info(s"ourIp updated to: $ourIp")

  // Override port with PORT env var if present
  private val port = sys.env.get("PORT").map { sPort =>
    sPort.toInt
  }.getOrElse(httpPort)

  val hostPort:String = s"${currentIp.ip}:$port"
  val nodeIdentity: NodeIdentity = NodeIdentity(currentIp.ip, port)
  val portAndInstance = PortAndInstance(port)



case class AnIpAddress(interfaceName: String, ip: String):
  def hasIp:Boolean = ip.nonEmpty

object AnIpAddress:
  def apply(interface: NetworkInterface): AnIpAddress =
    val ip = interface.getInetAddresses.asScala
      .find(_.isInstanceOf[Inet4Address])
      .map(_.getHostAddress)
      .getOrElse("")
    AnIpAddress(interface.getName, ip)