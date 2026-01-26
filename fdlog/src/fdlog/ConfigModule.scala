
package fdlog

import _root_.io.github.classgraph.{ClassGraph, ClassInfoList}
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.{Config, ConfigFactory}
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

import scala.jdk.CollectionConverters.CollectionHasAsScala


class ConfigModule() extends AbstractModule with ScalaModule:
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

    //primaryConfig is config/sarasec.conf, overrides the defaults in application.conf (resource)
    val primaryConfig = ConfigFactory.parseFile((os.pwd / "config" / "sarasec.conf").toIO)
    val defaultConfig: Config = ConfigFactory.load()
    val config: Config = primaryConfig.withFallback(defaultConfig)

    val entries = config.entrySet().asScala.toSeq
    for (entry <- entries) {
      val key = entry.getKey
      val value = config.getAnyRef(key)

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
