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

import fdswarm.fx.sections.SectionsProvider
import jakarta.inject.{Inject, Named}
import scalafx.scene.control.TitledPane
import scalafx.scene.layout.VBox

class ContestConfigPane @Inject() (
    contestConfigManager: ContestConfigManager,
    contestCatalog: ContestCatalog,
    sectionsProvider: SectionsProvider,
    qsoStore: fdswarm.store.QsoStore,
    filenameStamp: fdswarm.util.FilenameStamp,
    @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
) :
//  gridLinesVisible = true


  //  val contestButtons = contestTypeGroup.box
  //    val
  //  vBox.children += contestTypeGroup.box

  def pane(): TitledPane =
    val current: ContestConfig = contestConfigManager.configProperty.value

    val contestChooserPane = ContestType.chooseContest(current.contestType)


    new TitledPane {
      text = "Contest at this Node"
      content = new VBox {
        children = contestChooserPane

      }
    }
