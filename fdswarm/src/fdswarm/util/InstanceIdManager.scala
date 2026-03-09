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

import fdswarm.io.DirectoryProvider
import fdswarm.util.Ids.Id
import io.circe.*
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging

@Singleton
class InstanceIdManager @Inject()(directoryProvider: DirectoryProvider) extends LazyLogging:
  private val dir = directoryProvider()
  private val file = dir / "instance.json"

  var ourInstanceId: Id = loadOrCreate()

  private def loadOrCreate(): Id =
    if os.exists(file) then
      try {
        val content = os.read(file)
        parser.decode[InstanceConfig](content) match {
          case Right(config) => 
            logger.info(s"Loaded instance ID from $file: ${config.instanceId}")
            config.instanceId
          case Left(error) =>
            logger.error(s"Failed to decode $file: $error. Generating new one.")
            generateAndSave()
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error reading $file: ${e.getMessage}. Generating new one.")
          generateAndSave()
      }
    else
      generateAndSave()

  private def generateAndSave(): Id =
    val id = Ids.generateInstanceId()
    val config = InstanceConfig(id)
    os.makeDir.all(dir)
    os.write.over(file, config.asJson.spaces2)
    logger.info(s"Generated and saved new instance ID to $file: $id")
    id


case class InstanceConfig(instanceId: Id) derives Codec.AsObject
