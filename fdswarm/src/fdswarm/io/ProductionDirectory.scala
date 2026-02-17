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

package fdswarm.io

import jakarta.inject.Singleton

@Singleton
class ProductionDirectory extends DirectoryProvider:
  def apply(): os.Path = 
    val base = os.home / "fdswarm"
    sys.env.get("PORT") match
      case Some(port) => base / port
      case None => base

/**
 * Allows test TestDiretory for tests.
 */
trait DirectoryProvider :
  def apply(): os.Path
