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

package fdswarm.model

import fdswarm.util.Ids.Id
import fdswarm.util.NodeIdentity
import io.circe.Codec
import sttp.tapir.Schema

/** Header containing node metadata for API responses. */
case class NodeHeader(version: String, hostAndPort: NodeIdentity, udpInstanceId: Id) derives Codec.AsObject, Schema

/** Generic wrapper for API responses including a NodeHeader. */
case class ApiResponse[T](
    header: NodeHeader,
    data: T
) derives Schema

object ApiResponse {
  import io.circe.*
  given [T: Encoder]: Encoder[ApiResponse[T]] = Encoder.AsObject.derived
  given [T: Decoder]: Decoder[ApiResponse[T]] = Decoder.derived
}
