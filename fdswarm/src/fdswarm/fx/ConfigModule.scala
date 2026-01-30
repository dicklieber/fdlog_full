
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

import _root_.io.github.classgraph.{ClassGraph, ClassInfoList}
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.{DirectoryProvider, ProductionDirectory}
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

import scala.jdk.CollectionConverters.CollectionHasAsScala


class ConfigModule() extends AbstractModule with ScalaModule with LazyLogging:
  val config: Config = com.typesafe.config.ConfigFactory.load() // Reads application.conf


  override def configure(): Unit =

    // scan only where your implementations live
    val pkgs = Seq("sarasec")

    // unnamed set (inject with java.util.Set[Plugin])
//    AutoBind.bindAllImplementationsOf[EndPointSource](
//      binder = binder(),
//      packagesOnly = pkgs,
//      named = None, // or Some("plugins") for a named set
//      asSingleton = true // optional
//    )

//    bind[TickerApi].to[Ticker].asEagerSingleton()
//    bind[FilesManager].toInstance(new FilesManager(None))
      bind[DirectoryProvider].toInstance(new ProductionDirectory)
    //primaryConfig is config/sarasec.conf, overrides the defaults in application.conf (resource)
    val primaryConfig = ConfigFactory.parseFile((os.pwd / "config" / "sarasec.conf").toIO)
    val defaultConfig: Config = ConfigFactory.load()
    val config: Config = primaryConfig.withFallback(defaultConfig)

    val entries = config.entrySet().asScala.toSeq
    for (entry <- entries) {
      val key = entry.getKey
      val value = config.getAnyRef(key)
      logger.trace(s"Config entry: $key = $value type: ${value.getClass}")
      // Determine type and bind accordingly
      value match {
        case s: String =>
          bind[String]
            .annotatedWith(Names.named(key))
            .toInstance(s)

        case i: Integer =>
          bind(classOf[Int])
            .annotatedWith(Names.named(key))
            .toInstance(i.intValue)

        case l: java.lang.Long =>
          bind(classOf[Long])
            .annotatedWith(Names.named(key))
            .toInstance(l)

        case d: java.lang.Double =>
          bind(classOf[Double])
            .annotatedWith(Names.named(key))
            .toInstance(d)

        case b: java.lang.Boolean =>
          bind(classOf[Boolean])
            .annotatedWith(Names.named(key))
            .toInstance(b)

        case _ =>
        // Optionally log or ignore unsupported types
      }
    }

    // Optionally bind the entire config too
    bind(classOf[Config]).toInstance(config)
   

object ConfigModule:
  given Conversion[String, os.Path] = (in: String) => os.Path(in)
