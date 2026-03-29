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

package fdswarm.fx.contest

import fdswarm.fx.UserConfig
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.scene.control.{CheckBox, Label}
import scalafx.scene.layout.HBox

final class ExchangePane @Inject()(val userConfig: UserConfig) :

  def pane(contestConfig: ObjectProperty[ContestConfig]): HBox = {
    val phoneticProp: BooleanProperty = userConfig.getProperty[BooleanProperty]("usePhonetic")

    val weAreText = Bindings.createStringBinding(
      () => contestConfig.value.weAre(phoneticProp.value),
      contestConfig.delegate,
      phoneticProp.delegate
    )

    new HBox(spacing = 8) {
      children ++= Seq(
        new Label {
          text <== weAreText
        },
        new CheckBox {
          text = "Use Phonetic"
          selected <==> phoneticProp
        }
      )
    }
  }
