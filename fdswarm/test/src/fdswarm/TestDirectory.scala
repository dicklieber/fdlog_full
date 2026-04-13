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

package fdswarm

import fdswarm.logging.LazyStructuredLogging
import fdswarm.io.DirectoryProvider

class TestDirectory extends DirectoryProvider with LazyStructuredLogging:
  val tmpPath: os.Path = os.temp.dir(prefix = "testFdSwarm")
  logger.info(s"Created TestDirectory at $tmpPath")

  override def apply(): os.Path = tmpPath

  def cleanup(): Unit =
    logger.info(s"Cleaning up TestDirectory at $tmpPath")
    os.remove.all(tmpPath)
