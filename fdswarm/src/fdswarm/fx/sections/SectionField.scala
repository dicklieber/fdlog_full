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

package fdswarm.fx.sections

import fdswarm.fx.NextField
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.TextField

@Singleton
class SectionField @Inject()(sectionsProvider: SectionsProvider) extends TextField with NextField:
  
  text.onChange { (_, _, nv) =>
    validProperty.value = isValid(nv)
  }

  override def isValid(str: String): Boolean =
    if str == null then false
    else
      val upper = str.trim.toUpperCase
      sectionsProvider.allSections.exists(_.code.toUpperCase == upper)
