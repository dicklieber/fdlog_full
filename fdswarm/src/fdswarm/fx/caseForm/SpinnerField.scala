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

import scalafx.scene.Node
import scalafx.scene.control.{Control, Spinner, TextFormatter}

/**
 * A field that is a Spinner.
 */
final class SpinnerField(
                               min: Int = 0,
                               max: Int = Integer.MAX_VALUE
) extends FieldHandler:
  private var spinner: Option[Spinner[Int]] = None
    
    /* // todo force only digits
          editor.value.textFormatter = new TextFormatter[String]((change: TextFormatter.Change) => {
            if (change.isContentChange) {
              val newText = change.controlNewText
              if (newText.matches("-?\\d*")) {
                change
              } else {
                null
              }
            } else {
              change
            }
          })
    */
//  }

    /**
   * The control that is used to edit the field.
   *
   * @return
   */
  override def control(): Option[Node] = spinner 

  override def initialValue(value: Any): Unit =
    val int = value.asInstanceOf[Int]
    val sp = new Spinner[Int](min, max, int)
    spinner = Option(sp)
    setControl(sp)

  override def getValue: Any =
    spinner.get.value.value

