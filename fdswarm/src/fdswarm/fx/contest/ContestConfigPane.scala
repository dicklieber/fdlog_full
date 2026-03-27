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
import fdswarm.fx.utils.CaseClassPropertyEditor
import jakarta.inject.{Inject, Named}
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.TitledPane
import scalafx.scene.layout.{HBox, Pane, VBox}

class ContestConfigPane @Inject() (
                                    contestConfigManager: ContestConfigManager,
                                    contestCatalog: ContestCatalog,
                                    sectionsProvider: SectionsProvider,
                                    qsoStore: fdswarm.store.QsoStore,
                                    filenameStamp: fdswarm.util.FilenameStamp,
                                    @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                  ) :

  private val configEditor =
    new CaseClassPropertyEditor[ContestConfig](contestConfigManager.configProperty)

  private val current: ContestConfig =
    contestConfigManager.configProperty.value

  private val contestTypeProperty =
    ObjectProperty[ContestType](current.contestType)

  private val contestTypeBridge =
    configEditor.objectProperty("contestType")

  contestTypeBridge.value match
    case ct: ContestType =>
      if contestTypeProperty.value != ct then
        contestTypeProperty.value = ct
    case _ =>
      contestTypeBridge.value = contestTypeProperty.value

  contestTypeProperty.onChange { (_, _, newValue) =>
    if newValue != null && contestTypeBridge.value != newValue then
      contestTypeBridge.value = newValue
  }

  contestTypeBridge.onChange { (_, _, newValue) =>
    newValue match
      case ct: ContestType =>
        if contestTypeProperty.value != ct then
          contestTypeProperty.value = ct
      case _ =>
  }

  val contestChooserPane: Pane =
    ContestType.chooseContest(contestTypeProperty)

  val contestDetailPane: Pane =
    new VBox:
      spacing = 8
      children = Seq(
        configEditor.horizontalFormExcluding(Set("contestType")),
        configEditor.saveButton
      )

  def pane: TitledPane =
    new TitledPane:
      text = "Contest at this Node"
      content = new HBox:
        spacing = 12
        children.addAll(
          contestChooserPane,
          contestDetailPane
        )

  def result: ContestConfig =
    configEditor.save()
    contestConfigManager.configProperty.value
    contestConfigManager.configProperty.value