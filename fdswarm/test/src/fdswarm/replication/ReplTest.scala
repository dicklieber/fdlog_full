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

package fdswarm.replication

import fdswarm.fx.qso.FdHour
import fdswarm.store.{FdHourDigest, QsoStore}
import fdswarm.util.HostAndPort
import munit.FunSuite
import java.util.concurrent.LinkedBlockingQueue

class ReplTest extends FunSuite:

  test("Repl should process messages from queue and log needed FdHours"):
    val qsoStore = new QsoStore(new fdswarm.io.DirectoryProvider:
      override def apply(): os.Path = os.temp.dir()
    )
    
    // Setup some data in QsoStore if needed, but neededQsos can work with empty store
    
    val nodeStatusReceiver = new NodeStatusReceiver(8888):
      override val queue = new LinkedBlockingQueue[Array[Byte]]()
      override def start(): Unit = () // don't actually start the socket
      override def stop(): Unit = ()

    val repl = new Repl(qsoStore, nodeStatusReceiver)
    
    val fdHour = FdHour(15, 10)
    val digest = FdHourDigest(fdHour, 1, "some-digest")
    val statusMsg = StatusMessage(HostAndPort("1.2.3.4", 5555), Seq(digest))
    
    nodeStatusReceiver.queue.offer(statusMsg.toPacket)
    
    // We can't easily check the logger, but we can verify it doesn't crash 
    // and we can use a custom QsoStore to verify it was called.
    
    var calledWith: Seq[FdHourDigest] = Nil
    val mockQsoStore = new QsoStore(new fdswarm.io.DirectoryProvider:
      override def apply(): os.Path = os.temp.dir()
    ):
      override def neededQsos(incoming: Seq[FdHourDigest]): Seq[FdHour] =
        calledWith = incoming
        super.neededQsos(incoming)

    val replWithMock = new Repl(mockQsoStore, nodeStatusReceiver)
    replWithMock.start()
    
    // Wait a bit for the thread to process
    Thread.sleep(200)
    
    assertEquals(calledWith.size, 1)
    assertEquals(calledWith.head, digest)
    
    replWithMock.stop()
