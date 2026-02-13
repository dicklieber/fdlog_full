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

package fdswarm.fx.caseForm

import scalafx.scene.control.ComboBox

/**
 * A "field value" that knows:
 *  - the current selected value
 *  - how to build a ComboBox with the valid choices (from anywhere: store, catalog, db, etc.)
 *
 * This is meant to be embedded in your case class, like enum fields are.
 */
final case class ChoiceField[A <: AnyRef](
                                           value: A,
                                           build: Option[A] => ComboBox[A]
                                         ):
  def comboBox(): ComboBox[A] =
    build(Some(value))

  def withValue(v: A): ChoiceField[A] =
    copy(value = v)