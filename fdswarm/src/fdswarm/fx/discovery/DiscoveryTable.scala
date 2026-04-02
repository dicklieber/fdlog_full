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
import fdswarm.fx.contest.{ContestConfigPane, ContestConfigPaneProvider}
import scalafx.Includes.*
import scalafx.scene.control.{Button, TitledPane}

class DiscoveryTable(contestConfigPane: ContestConfigPane) extends TitledPane with LazyLogging:
  text = "Discovered FdSwarm Nodes"
  collapsible = false



  def setItems(items: Seq[NodeContestStation]): Unit =
    content = NodeContestStation.buildTable(items)
