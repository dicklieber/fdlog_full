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

import fdswarm.fx.caseForm.*
import fdswarm.fx.sections.SectionsProvider
import jakarta.inject.{Inject, Named}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ComboBox, Label, ListCell, ListView, TableColumn, TableView}
import scalafx.scene.layout.{GridPane, Pane, Region}
import scalafx.util.StringConverter

class ContestConfigPane @Inject() (
    contestConfigManager: ContestConfigManager,
    contestCatalog: ContestCatalog,
    sectionsProvider: SectionsProvider,
    qsoStore: fdswarm.store.QsoStore,
    filenameStamp: fdswarm.util.FilenameStamp,
    @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
) :
//  gridLinesVisible = true
//  private val contestConfig: ContestConfig = contestConfigManager.configProperty.value
//   field names
//  add(new Label("Contest"), 0, 0)
//  add(new TextField(contestConfig.contestType.toString), 1, 0)
//  for
//    col <- 0 until contestConfig.productArity
//  do
//    add(new Label(contestConfig.productElementName(col)), col, 0)
//    add(new Label(contestConfig.productElement(col).toString), col, 1)

//
  val myCaseForm: MyCaseForm[ContestConfig] = new MyCaseForm[ContestConfig](contestConfigManager.configProperty.value)

  val pane: Pane = myCaseForm.pane()
//  val sectionsList = sectionsProvider.allSections.sortBy(_.code)
//  // Setup listener for contest type change to prompt for year and update class choices
//  val contestCombo = new ComboBox[ContestType]("contest")
//  val classCombo = myCaseForm.control[ComboBox[String]]("ourClass")
//  val hasQsos = qsoStore.all.nonEmpty
//
//  val statusLabel = new Label:
//    visible = false
//    managed = false
//
//
//  // Initialize classCombo items
//  classCombo.items =
//    contestConfigManager.contestConfig
//    ObservableBuffer.from(getClasses(contestCombo.value.value).map(_.ch))
//
//  contestCombo.onAction = _ =>
//    val newType: ContestType = contestCombo.value.value
//    val newClasses = getClasses(newType)
//    classCombo.items = ObservableBuffer.from(newClasses.map(_.ch))
//    // fully reset selection to the first valid option for the new contest (if any)
//    if newClasses.nonEmpty then classCombo.value = newClasses.head.ch
//    else classCombo.value = null
//  var currentContestType: ContestType = config.contestType
//
//  def transmittersSpinnerField(currentTransmitters: Int): SpinnerField =
//    SpinnerField(currentTransmitters, 1, 100)

//  def classChoiceField(currentClassCode: String): ChoiceField[String] =
//    ChoiceField(
//      currentClassCode,
//      currentVal =>
//        new ComboBox[String](
//          ObservableBuffer.from(getClasses(currentContestType).map(_.ch))
//        ):
//          editable = false
//          currentVal.foreach(v => value = v)
//          converter = new StringConverter[String]:
//            override def toString(ch: String): String =
//              if ch == null then "" else ch
//            override def fromString(s: String): String =
//              if s == null then "" else s
//          cellFactory = (lv: ListView[String]) =>
//            new ListCell[String]:
//              item.onChange { (_, _, it) =>
//                text =
//                  if it == null then ""
//                  else
//                    getClasses(currentContestType)
//                      .find(_.ch == it)
//                      .map(c => s"${c.ch} - ${c.description}")
//                      .getOrElse(it)
//              }
//          buttonCell = new ListCell[String]:
//            item.onChange { (_, _, it) =>
//              text = if it == null then "" else it
//            }
//          prefWidth <== scalafx.beans.binding.Bindings.createDoubleBinding(
//            () =>
//              val strings = getClasses(currentContestType).map(_.ch)
//              val textObj = new scalafx.scene.text.Text()
//              val maxW = strings
//                .map { s =>
//                  textObj.text = s
//                  textObj.getLayoutBounds.getWidth
//                }
//                .maxOption
//                .getOrElse(0.0)
//              maxW + 60.0
//            ,
//            items
//          )
//          maxWidth = Region.USE_PREF_SIZE
//          minWidth = Region.USE_PREF_SIZE
//    )

//  def getClasses(contestType: ContestType): Seq[ClassChoice] =
//    contestCatalog.contests
//      .find(_.name == contestType)
//      .map(_.classChars)
//      .getOrElse(Seq.empty)

//  def sectionChoiceField(currentSectionCode: String): ChoiceField[String] =
//    ChoiceField(
//      currentSectionCode,
//      currentVal =>
//        new ComboBox[String](ObservableBuffer.from(sectionsList.map(_.code))):
//          editable = false
//          currentVal.foreach(v => value = v)
//          converter = new StringConverter[String]:
//            override def toString(code: String): String =
//              if code == null then "" else code
//            override def fromString(s: String): String =
//              if s == null then "" else s
//          cellFactory = (lv: ListView[String]) =>
//            new ListCell[String]:
//              item.onChange { (_, _, it) =>
//                text =
//                  if it == null then ""
//                  else
//                    sectionsList
//                      .find(_.code == it)
//                      .map(s => s"${s.code} - ${s.name}")
//                      .getOrElse(it)
//              }
//          buttonCell = new ListCell[String]:
//            item.onChange { (_, _, it) =>
//              text = if it == null then "" else it
//            }
//          prefWidth <== scalafx.beans.binding.Bindings.createDoubleBinding(
//            () =>
//              val strings = sectionsList.map(_.code)
//              val textObj = new scalafx.scene.text.Text()
//              val maxW = strings
//                .map { s =>
//                  textObj.text = s
//                  textObj.getLayoutBounds.getWidth
//                }
//                .maxOption
//                .getOrElse(0.0)
//              maxW + 60.0
//            ,
//            items
//          )
//          maxWidth = Region.USE_PREF_SIZE
//          minWidth = Region.USE_PREF_SIZE
//    )
