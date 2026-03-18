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

package manager

import munit.FunSuite
import os.*

class AppInstanceTest extends FunSuite:

  test("AppInstance spawns and stops"):
    // Create a temporary "debugConfigJson" file
    val tempFile = os.temp(contents = "{}", suffix = ".json", deleteOnExit = true)

    // For this test, we don't need a real jar if we just want to see it fail to EXECUTE.
    // However, the original issue was "No such file or directory" because of how os.proc was called.
    // With the fix, it should at least try to run "java".

    val app = new AppInstance(tempFile.toString, 8080)
    Thread.sleep(3000)
    assert(app.subProcess.isAlive())

    app.stop()
    app.subProcess.waitFor(5000)
    assert(!app.subProcess.isAlive())
