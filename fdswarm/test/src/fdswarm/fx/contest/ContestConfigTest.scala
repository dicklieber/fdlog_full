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

import fdswarm.model.Callsign
import munit.FunSuite
import scalafx.scene.control.Label
import scalafx.scene.layout.GridPane

class ContestConfigTest extends FunSuite:

  test("exchange formats transmitters class section") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    assertEquals(config.exchange, "2O CT")
  }

  test("weAre formats with phonetics") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val weAre = config.weAre(true)
    assertEquals(weAre, "We are Whiskey One Alpha Whiskey 2 Oscar Charlie Tango")
  }

  test("weAre handles null callsign without throwing") {
    val config = ContestConfig(
      contestType = ContestType.WFD,
      ourCallsign = null,
      transmitters = 2,
      ourClass = "O",
      ourSection = "CT"
    )
    val weAre = config.weAre(
      usePhonetic = false
    )
    assertEquals(
      weAre,
      "We are  2O CT"
    )
  }

  test("exchange single transmitter") {
    val config = ContestConfig(ContestType.ARRL, Callsign("K1ABC"), 1, "1A", "NH")
    assertEquals(config.exchange, "11A NH")
  }

  test("toolTip formats contest config as key value grid with details") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")

    assertEquals(
      toolTipLabelTexts(config),
      Seq(
        "contestType",
        "WFD",
        "callsign",
        "W1AW",
        "transmitters",
        "2",
        "ourClass",
        "O (Outdoor)",
        "ourSection",
        "CT (Connecticut)"
      )
    )
  }

  test("toolTip handles missing callsign and unknown details") {
    val config = ContestConfig(
      contestType = ContestType.NONE,
      ourCallsign = null,
      transmitters = 1,
      ourClass = "-",
      ourSection = "-"
    )

    assertEquals(
      toolTipLabelTexts(config),
      Seq(
        "contestType",
        "NONE",
        "callsign",
        "",
        "transmitters",
        "1",
        "ourClass",
        "-",
        "ourSection",
        "-"
      )
    )
  }

  test("ContestConfig implements ContestConfigFields") {
    val config: ContestConfigFields = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    assertEquals(config.contestType, ContestType.WFD)
    assertEquals(config.ourCallsign, Callsign("W1AW"))
    assertEquals(config.transmitters, 2)
    assertEquals(config.ourClass, "O")
    assertEquals(config.ourSection, "CT")
  }

  private def toolTipLabelTexts(config: ContestConfig): Seq[String] =
    val grid = config.toolTip.asInstanceOf[GridPane]
    grid.children.map(_.asInstanceOf[Label].text.value).toSeq
