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

package fdswarm.client

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import fdswarm.store.QsoStore
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import net.codingwell.scalaguice.ScalaModule
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import scala.jdk.CollectionConverters.CollectionHasAsScala

class ClientModule extends AbstractModule with ScalaModule with LazyLogging {
  override def configure(): Unit = {
    val config: Config = ConfigFactory.load()
    bind[Config].toInstance(config)

    // Bind all config entries to @Named properties, similar to ConfigModule
    val entries = config.entrySet().asScala.toSeq
    for (entry <- entries) {
      val key = entry.getKey
      try {
        val value = config.getAnyRef(key)
        value match {
          case s: String =>
            bind[String].annotatedWith(Names.named(key)).toInstance(s)
          case i: java.lang.Integer =>
            bind[Int].annotatedWith(Names.named(key)).toInstance(i.intValue)
          case l: java.lang.Long =>
            bind[Long].annotatedWith(Names.named(key)).toInstance(l)
          case d: java.lang.Double =>
            bind[Double].annotatedWith(Names.named(key)).toInstance(d)
          case b: java.lang.Boolean =>
            bind[Boolean].annotatedWith(Names.named(key)).toInstance(b)
          case _ =>
          // Ignore other types
        }
      } catch {
        case e: Exception =>
          logger.trace(s"Could not bind config key $key: ${e.getMessage}")
      }
    }

    bind[DirectoryProvider].to[ClientDirectoryProvider].asEagerSingleton()
    bind[QsoStore].to[RestQsoStore].asEagerSingleton()
    bind[RestQsoStore].asEagerSingleton()

    val meterRegistry = new CompositeMeterRegistry()
    bind[MeterRegistry].toInstance(meterRegistry)

    val httpClient = EmberClientBuilder.default[IO].build.allocated.unsafeRunSync()._1
    bind(new com.google.inject.TypeLiteral[Client[IO]]() {}).toInstance(httpClient)

    bind[FdSwarmRestClient].asEagerSingleton()
  }
}
