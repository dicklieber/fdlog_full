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

package fdswarm.fx.components

import fdswarm.model.ChoiceItem
import munit.FunSuite

class AnyComboBoxTest extends FunSuite:

  test(
    "formatValue handles null"
  ) {
    assertEquals(
      AnyComboBox.formatValue[String](
        null,
        "-any-"
      ),
      ""
    )
  }

  test(
    "parseValue handles null"
  ) {
    assertEquals(
      AnyComboBox.parseValue[String](
        null,
        "-any-",
        Seq(
          ChoiceItem("A"),
          ChoiceItem("B")
        )
      ),
      None
    )
  }
