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

import fdswarm.{ContestDateCalculator, ContestDates}
import fdswarm.model.Callsign
import io.circe.Codec
import fdswarm.util.JavaTimeCirce.given
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.{RadioButton, ToggleGroup}
import scalafx.scene.layout.{Pane, VBox}
import io.circe.{Decoder, Encoder}

import java.time.*

enum ContestType(val name: String, val compute: Int => ContestDates) derives sttp.tapir.Schema:
  def dates(year: Int): ContestDates = compute(year)

  case WFD extends ContestType("Winter Field Day", ContestDateCalculator.lastFull)
  case ARRL extends ContestType("ARRL Field Day", ContestDateCalculator.forthFullWeekend)

object ContestType:

  def chooseContest(value:ObjectProperty[ContestType]): Pane =
    val tg = new ToggleGroup()

    val buttons: Seq[(ContestType, RadioButton)] =
      ContestType.values.toSeq.map: contestType =>
        val button = new RadioButton:
          text = contestType.name
          toggleGroup = tg
        contestType -> button

    buttons.find(_._1 == value.value).foreach: (_, button) =>
      button.selected = true

    tg.selectedToggle.onChange { (_, _, newToggle) =>
      if newToggle != null then
        buttons.find(_._2 == newToggle).foreach: (contestType, _) =>
          if value.value != contestType then
            value.value = contestType
    }

//    current.onChange { (_, _, newValue) =>
//      buttons.find(_._1 == newValue).foreach: (_, button) =>
//        if tg.selectedToggle.value != button then
//          tg.selectToggle(button)
//    }

    new VBox:
      spacing = 8
      children = buttons.map(_._2)
  given Codec[ContestType] = Codec.from(
    Decoder.decodeString.emap(s =>
      try Right(ContestType.valueOf(s))
      catch case _: IllegalArgumentException => Left(s"Invalid ContestType: $s")
    ),
    Encoder.encodeString.contramap(_.toString)
  )

case class ContestTimes(start: ZonedDateTime, end: ZonedDateTime)
