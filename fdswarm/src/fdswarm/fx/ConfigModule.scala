
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

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fdswarm.AutoBind
import fdswarm.api.ApiEndpoints
import fdswarm.io.{DirectoryProvider, ProductionDirectory}
import fdswarm.store.{QsoStore, ReplicationSupport}
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import _root_.meters4s.Reporter
import net.codingwell.scalaguice.ScalaModule
import com.google.inject.TypeLiteral
import fdswarm.replication.status.{SwarmStatus, SwarmStatusApi}
import fdswarm.replication.{BroadcastTransport, MulticastTransport, NodeStatusHandler, StatusBroadcastService, Transport}

import scala.jdk.CollectionConverters.CollectionHasAsScala


class ConfigModule() extends AbstractModule with ScalaModule with LazyLogging:

  override def configure(): Unit =
    val directoryProvider = new ProductionDirectory
    bind[DirectoryProvider].toInstance(directoryProvider)
    fdswarm.util.LoggingConfigurator.addFileAppender(directoryProvider)

    val loggingManager = new fdswarm.util.LoggingManager(directoryProvider)
    loggingManager.applyInitialConfig()
    bind[fdswarm.util.LoggingManager].toInstance(loggingManager)

    bind[fdswarm.util.NodeIdentityManager].asEagerSingleton()

    val pkgs = Seq("fdswarm.api", "fdswarm.grafana", "fdswarm.web")
    val allPkgs = Seq("fdswarm")

    // unnamed set (inject with java.util.Set[ApiEndpoints])
    AutoBind.bindAllImplementationsOf[ApiEndpoints](
      binder = binder(),
      packagesOnly = pkgs,
      named = None,
      asSingleton = true
    )

    val discoveredLoggers = AutoBind.discoverImplementationsOf[LazyLogging](allPkgs)
    bind[Seq[String]].annotatedWith(Names.named("discoveredLoggerNames")).toInstance(discoveredLoggers)

    val primaryConfigFromFile = ConfigFactory.parseFile((os.pwd / "config" / "sarasec.conf").toIO)
    val defaultConfigFromFile: Config = ConfigFactory.load()
    val fullConfig: Config = primaryConfigFromFile.withFallback(defaultConfigFromFile)
    bind[SwarmStatusApi].to[SwarmStatus]
    bind[StatusBroadcastService].asEagerSingleton()
    bind[NodeStatusHandler].asEagerSingleton()
    bind[MulticastTransport].asEagerSingleton()
    bind[BroadcastTransport].asEagerSingleton()

    val transportType = if fullConfig.hasPath("fdswarm.transportType") then fullConfig.getString("fdswarm.transportType") else "Multicast"
    if (transportType.equalsIgnoreCase("Broadcast")) {
      bind[Transport].to[BroadcastTransport].asEagerSingleton()
    } else {
      bind[Transport].to[MulticastTransport].asEagerSingleton()
    }

    bind[QsoStore].to[ReplicationSupport].asEagerSingleton()
    bind[MeterRegistry].to[PrometheusMeterRegistry].asEagerSingleton()
    val prometheusRegistry = new PrometheusMeterRegistry(io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT)
    bind[PrometheusMeterRegistry].toInstance(prometheusRegistry)
    val reporter = Reporter.fromRegistry[IO](prometheusRegistry).unsafeRunSync()
    bind(new TypeLiteral[Reporter[IO]](){}).toInstance(reporter)

    val entries = fullConfig.entrySet().asScala.toSeq
    for (entry <- entries) {
      val key = entry.getKey
      val value = fullConfig.getAnyRef(key)
      logger.trace(s"Config entry: $key = $value type: ${value.getClass}")
      // Determine type and bind accordingly
      value match {
        case s: String =>
          bind[String]
            .annotatedWith(Names.named(key))
            .toInstance(s)

        case i: Integer =>
          bind[Int]
            .annotatedWith(Names.named(key))
            .toInstance(i.intValue)

        case l: java.lang.Long =>
          bind[Long]
            .annotatedWith(Names.named(key))
            .toInstance(l)

        case d: java.lang.Double =>
          bind[Double]
            .annotatedWith(Names.named(key))
            .toInstance(d)

        case b: java.lang.Boolean =>
          bind[Boolean]
            .annotatedWith(Names.named(key))
            .toInstance(b)

        case _ =>
        // Optionally log or ignore unsupported types
      }
    }

    // Optionally bind the entire config too
    bind[Config].toInstance(fullConfig)


object ConfigModule:
  given Conversion[String, os.Path] = (in: String) => os.Path(in)
