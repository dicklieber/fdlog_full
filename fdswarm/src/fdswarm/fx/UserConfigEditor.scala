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

import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.{BooleanProperty, IntegerProperty, Property, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.VBox
import scalafx.stage.Window

@Singleton
final class UserConfigEditor @Inject()(userConfig: UserConfig) {

  case class ConfigRow(name: String, property: Property[?, ?]) {
    val nameProp = new StringProperty(this, "name", name)
  }

  def show(ownerWindow: Window): Unit = {
    val rows = ObservableBuffer.from(userConfig.getProperties.map { case (k, v) => ConfigRow(k, v) }.toSeq.sortBy(_.name))

    lazy val tv: TableView[ConfigRow] = new TableView[ConfigRow](rows) {
      columns ++= Seq(
        new TableColumn[ConfigRow, String] {
          text = "Setting"
          cellValueFactory = { _.value.nameProp }
          prefWidth = 200
        },
        new TableColumn[ConfigRow, Any] {
          text = "Value"
          cellValueFactory = { _.value.property.asInstanceOf[Property[Any, Any]] }
          cellFactory = (col: TableColumn[ConfigRow, Any]) => new TableCell[ConfigRow, Any] {
            item.onChange { (_, _, newValue) =>
              if (newValue != null) {
                val row = tv.items.value(index.value)
                row.property match {
                  case bp: BooleanProperty =>
                    val cb = new CheckBox {
                      selected <==> bp
                    }
                    graphic = cb
                  case ip: IntegerProperty =>
                    val tf = new TextField {
                      text = ip.value.toString
                      text.onChange { (_, _, newVal) =>
                        try {
                          ip.value = newVal.toInt
                        } catch {
                          case _: NumberFormatException => // ignore
                        }
                      }
                    }
                    graphic = tf
                  case _ =>
                    text = newValue.toString
                    graphic = null
                }
              } else {
                text = null
                graphic = null
              }
            }
          }
          prefWidth = 150
          editable = true
        }
      )
    }

    val dialog = new Dialog[Unit] {
      title = "User Configuration"
      headerText = "Edit User Settings"
      initOwner(ownerWindow)
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.dialogPane().content = new VBox {
      spacing = 10
      padding = Insets(10)
      prefWidth = 400
      prefHeight = 300
      children = Seq(tv)
    }

    dialog.showAndWait()
  }
}
