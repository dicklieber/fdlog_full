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

package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.ContestConfigPaneProvider
import fdswarm.replication.NodeStatus
import scalafx.scene.control.TitledPane

class DiscoveryTable(contestConfigPaneProvider: ContestConfigPaneProvider,
                     onTableUpdated: () => Unit = () => ())
  extends TitledPane with LazyLogging:
  text = "Discovered FdSwarm Nodes"
  collapsible = false

  private val provider = new ReceivedNodeStatusProvider(contestConfigPaneProvider)


  def setItems(items: Seq[NodeStatus]): Unit =
    val byStamp = items.sortBy(_.statusMessage.contestConfig.stamp).reverse
    content = ReceivedNodeStatusTable.buildTable(byStamp, provider)
    onTableUpdated()
