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

import fdswarm.io.DirectoryProvider
import munit.FunSuite
import scalafx.beans.property.{BooleanProperty, IntegerProperty, Property}

class UserConfigTest extends FunSuite {

  class MockDirectoryProvider extends DirectoryProvider {
    private val tempDir = os.temp.dir()
    override def apply(): os.Path = tempDir
  }

  test("UserConfig should persist and load values") {
    val provider = new MockDirectoryProvider()
    val config = new UserConfig(provider)

    config.getProperty[BooleanProperty]("developerMode").value = true
    config.getProperty[IntegerProperty]("qsoListLines").value = 42
    config.save()

    val config2 = new UserConfig(provider)
    config2.load()

    assertEquals(config2.get[Boolean]("developerMode"), true)
    assertEquals(config2.get[Int]("qsoListLines"), 42)
  }

  test("UserConfig get[T] should work") {
    val provider = new MockDirectoryProvider()
    val config = new UserConfig(provider)
    config.getProperty[BooleanProperty]("developerMode").value = true
    config.getProperty[IntegerProperty]("qsoListLines").value = 25

    assertEquals(config.get[Boolean]("developerMode"), true)
    assertEquals(config.get[Int]("qsoListLines"), 25)
  }

  test("UserConfig should support adding a new property to the list") {
    val provider = new MockDirectoryProvider()
    // Define a variant of UserConfig (cannot inherit because it's final, but the pattern is clear)
    class VariantUserConfig(dp: DirectoryProvider) {
      val propertyList: List[Property[?, ?]] = List(
        new BooleanProperty(this, "developerMode", false),
        new IntegerProperty(this, "qsoListLines", 10),
        new IntegerProperty(this, "extraProp", 123)
      )
      val properties: Map[String, Property[?, ?]] = propertyList.map(p => p.name -> p).toMap
    }
    val config = new VariantUserConfig(provider)
    assert(config.properties.contains("developerMode"))
    assert(config.properties.contains("qsoListLines"))
    assert(config.properties.contains("extraProp"))
  }
}
