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

package fdswarm.util
 
import fdswarm.fx.contest.{ContestCatalog, ContestConfig, ContestManager, ContestType}
import fdswarm.TestDirectory
import com.typesafe.config.ConfigFactory
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import munit.FunSuite
 
class FilenameStampTest extends FunSuite:
 
  private var testDir: TestDirectory = _
  private var filenameStamp: FilenameStamp = _
  private var contestManager: ContestManager = _
 
  override def beforeEach(context: BeforeEach): Unit =
    testDir = new TestDirectory()
    val config = ConfigFactory.parseString(
      """
        |fdswarm.contests = [
        |  {
        |    name = "WFD",
        |    classChars = [
        |      { ch = "O", description = "Outdoor" },
        |      { ch = "I", description = "Indoor" }
        |    ]
        |  }
        |]
        |""".stripMargin)
    val catalog = new ContestCatalog(config)
    contestManager = new ContestManager(testDir, catalog)
    filenameStamp = new FilenameStamp(contestManager)
 
  override def afterEach(context: AfterEach): Unit =
    testDir.cleanup()

  test("build() creates a filename using current contest"):
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    contestManager.setConfig(ContestConfig(ContestType.ARRL, now, now.plusHours(24)))
    
    val instant = Instant.parse("2026-02-25T12:00:00Z")
    val result = filenameStamp.build(instant)
    
    // ARRL.toString is ARRL
    // Assuming BuildInfo.name = "fdswarm" and dataVersion = "1.0.0" (or whatever is in properties)
    val parts = result.split('_')(0).split('-')
    assertEquals(parts(1), "ARRL")
    assert(result.endsWith("_20260225T120000Z"))
