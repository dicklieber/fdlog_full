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

import com.google.inject.Singleton
import com.typesafe.scalalogging.LazyLogging
import jakarta.inject.Inject


/**
 * Periodically broadcasts, via the [[MulticastTransport]] the current node's status to other nodes in the swarm.
 *
 * This service runs a background daemon thread that periodically (every `broadcastPeriodSec`)
 * fetches a gzipped JSON representation of the local hourly QSO digests from [[NodeStatusHandler]]
 * and broadcasts it as a UDP packet to the configured `broadcastAddress` and `statusPort`.
 *
 * The UDP packets are prefixed with a standard [[UDPHeader]] with [[Service.Status]].
 *
 * one */
@Singleton
class NodeStatusSender @Inject()(
                                  statusBroadcastService: StatusBroadcastService
                                ) extends LazyLogging:

  def start(): Unit =
    statusBroadcastService.start()

  def stop(): Unit =
    statusBroadcastService.stop()