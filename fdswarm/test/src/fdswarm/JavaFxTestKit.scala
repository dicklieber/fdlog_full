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

// src/test/scala/fx/JavaFxTestKit.scala
package fdswarm

import javafx.application.Platform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object JavaFxTestKit :
  private val started = AtomicBoolean(false)

  /** Call once before constructing any JavaFX Control (TextField, Label, etc.). */
  def init(): Unit =
    if (started.compareAndSet(false, true)) {
      val latch = CountDownLatch(1)
      Platform.startup(() => latch.countDown()) // starts toolkit + FX thread
      if (!latch.await(10, TimeUnit.SECONDS))
        throw new RuntimeException("JavaFX Platform.startup timed out")
    }

  /** Run code on the FX thread and wait for completion (useful for UI mutations). */
  def runOnFx[A](thunk: => A): A =
    init()
    if (Platform.isFxApplicationThread) thunk
    else {
      var result: Either[Throwable, A] = null
      val latch = CountDownLatch(1)
      Platform.runLater(() => {
        try result = Right(thunk)
        catch case t: Throwable => result = Left(t)
        finally latch.countDown()
      })
      if (!latch.await(10, TimeUnit.SECONDS))
        throw new RuntimeException("runOnFx timed out")
      result.fold(throw _, identity)
    }
