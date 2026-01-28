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

package fdswarm.fx

import scalafx.scene.control.Tooltip

trait PropertyCellName {
 def toolTip: String

 def name:String
}

object PropertyCellName {
 val noName: StringCellName = StringCellName("")
}

/**
 * //todo Probably need a [[PropertyCell]] that nas no name
 *
 * @param name
 * @param toolTip
 */
case class StringCellName(name:String, toolTip:String = "") extends PropertyCellName
