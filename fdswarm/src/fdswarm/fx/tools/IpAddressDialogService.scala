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

package fdswarm.fx.tools

import fdswarm.util.{AnIpAddress, NodeIdentityManager}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane
import scalafx.stage.Window

@Singleton
final class IpAddressDialogService @Inject() (
                                               nodeIdentityManager: NodeIdentityManager
                                             ) {

  def show(ownerWindow: Window): Unit = {
    val interfaces = nodeIdentityManager.suitableInterfaces
    val currentIp = nodeIdentityManager.currentIp

    val comboBox = new ComboBox[AnIpAddress](interfaces) {
      cellFactory = (list: ListView[AnIpAddress]) => new ListCell[AnIpAddress] {
        item.onChange { (_, _, newItem) =>
          text = if (newItem != null) s"${newItem.interfaceName}: ${newItem.ip}" else ""
        }
      }
      buttonCell = new ListCell[AnIpAddress] {
        item.onChange { (_, _, newItem) =>
          text = if (newItem != null) s"${newItem.interfaceName}: ${newItem.ip}" else ""
        }
      }
      value = currentIp
    }

    val dialog = new Dialog[AnIpAddress] {
      title = "Set IP Address"
      headerText = "Choose an IP address for this node"
      initOwner(ownerWindow)
    }

    val okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel, okButtonType)

    dialog.dialogPane().content = new GridPane {
      hgap = 10
      vgap = 10
      padding = Insets(10)
      add(new Label("Select IP:"), 0, 0)
      add(comboBox, 1, 0)
    }

    dialog.resultConverter = (btn: ButtonType) => {
      if (btn == okButtonType) comboBox.value.value else null
    }

    val result = dialog.delegate.showAndWait()
    if (result.isPresent && result.get() != null) {
      nodeIdentityManager.setIp(result.get().asInstanceOf[AnIpAddress])
    }
  }
}
