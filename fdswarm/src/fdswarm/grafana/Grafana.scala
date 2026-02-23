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

package fdswarm.grafana

import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import com.typesafe.scalalogging.LazyLogging
import fdswarm.api.ApiEndpoints
import fdswarm.model.Qso
import fdswarm.store.QsoStore
import jakarta.inject.*
import org.slf4j.LoggerFactory
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*

import java.time.Instant


// ===== Assumes your existing domain types =====
// case class Qso(callsign: Callsign, contestClass: String, section: String, bandMode: BandMode, qsoMetadata: QsoMetadata,
//                stamp: Instant = Instant.now(), uuid: Id = Ids.generateId())
//   derives io.circe.Codec.AsObject, sttp.tapir.Schema
//
// case class BandMode(band: Band, mode: Mode) { def cabMode: Band }
//   (you provided this; we use .band/.mode/.cabMode for grouping)

// ---------- FX Snapshot Helper ----------

object Fx:
  private val log = LoggerFactory.getLogger(getClass)

  /** Run `thunk` on the JavaFX thread and return the result in IO. */
  def onFxThread[A](thunk: => A): IO[A] =
    IO.deferred[Either[Throwable, A]].flatMap { d =>
      Platform.runLater {
        try d.complete(Right(thunk)).unsafeRunAndForget()
        catch case t: Throwable =>
          log.warn("FX-thread task failed", t)
          d.complete(Left(t)).unsafeRunAndForget()
      }
      d.get.rethrow
    }

// ---------- View over ObservableBuffer ----------

trait QsoView:
  def snapshotAll: IO[Vector[Qso]]

object QsoView:
  def fromObservableBuffer(buf: ObservableBuffer[Qso]): QsoView =
    new QsoView:
      def snapshotAll: IO[Vector[Qso]] =
        Fx.onFxThread(buf.toVector)

// ---------- API Response Models ----------

final case class RecentResponse(items: List[Qso]) derives io.circe.Codec.AsObject, sttp.tapir.Schema
final case class Series(metric: Map[String, String], points: List[(Instant, Long)]) derives io.circe.Codec.AsObject, sttp.tapir.Schema
final case class TimeseriesResponse(series: List[Series]) derives io.circe.Codec.AsObject, sttp.tapir.Schema
final case class SectionPoint(section: String, lat: Double, lon: Double, count: Long) derives io.circe.Codec.AsObject, sttp.tapir.Schema
final case class GeomapResponse(points: List[SectionPoint]) derives io.circe.Codec.AsObject, sttp.tapir.Schema

// ---------- Endpoints definition ----------

object QsoGrafanaEndpoints:

  private val base = endpoint.in("api" / "qso")

  /** Latest QSOs (good for a Grafana table panel). */
  val recent: PublicEndpoint[Int, Unit, RecentResponse, Any] =
    base.get.in("recent")
      .in(query[Int]("limit").default(500))
      .out(jsonBody[RecentResponse])

  /** Bucketed counts (good for time-series panels). */
  val timeseries: PublicEndpoint[(Instant, Instant, Int, String), Unit, TimeseriesResponse, Any] =
    base.get.in("timeseries")
      .in(query[Instant]("from"))
      .in(query[Instant]("to"))
      .in(query[Int]("stepSeconds").default(60))
      .in(query[String]("group").default("band,cabMode,section"))
      .out(jsonBody[TimeseriesResponse])

  /** Section counts with (lat, lon) for Grafana Geomap. */
  val geomapSections: PublicEndpoint[(Instant, Instant), Unit, GeomapResponse, Any] =
    base.get.in("geomap" / "sections")
      .in(query[Instant]("from"))
      .in(query[Instant]("to"))
      .out(jsonBody[GeomapResponse])

// ---------- Guice singleton that exposes ServerEndpoints ----------

@Singleton
final class QsoGrafanaApi @Inject() (
                                      // Adjust the injected type to match your actual QsoStore type/package.
                                      // This code assumes your store exposes:
                                      //   val qsoCollection: ObservableBuffer[Qso]
                                      qsoStore: QsoStore
                                    ) extends ApiEndpoints with LazyLogging:

  private val view: QsoView = QsoView.fromObservableBuffer(qsoStore.qsoCollection)

  // Keep label cardinality low for Grafana use. DO NOT add "callsign".
  private val allowedGroups = Set("band", "mode", "cabMode", "section", "contestClass")

  private def parseGroups(s: String): List[String] =
    s.split(',').toList.map(_.trim).filter(allowedGroups.contains)

  private def bucketStart(ts: Instant, stepSeconds: Int): Instant =
    val step = math.max(stepSeconds, 1).toLong
    val epoch = ts.getEpochSecond
    Instant.ofEpochSecond((epoch / step) * step)

  /** Section centroids for Geomap. Fill out as desired, or load from config.
   * With your scale (<= ~10k QSOs), even a full map is tiny. */
  private val sectionCentroids: Map[String, (Double, Double)] = Map(
    "IL" -> (40.00, -89.00),
    "IN" -> (40.00, -86.13),
    "WI" -> (44.50, -89.50)
  )

  private def bandOf(q: Qso): String = q.bandMode.band.toString
  private def modeOf(q: Qso): String = q.bandMode.mode.toString
  private def cabModeOf(q: Qso): String = q.bandMode.cabMode.toString

  private def metricKey(q: Qso, keys: List[String]): Map[String, String] =
    keys.foldLeft(Map.empty[String, String]) { (m, k) =>
      val v = k match
        case "band"         => bandOf(q)
        case "mode"         => modeOf(q)
        case "cabMode"      => cabModeOf(q)
        case "section"      => q.section
        case "contestClass" => q.contestClass
        case _              => "unknown"
      m.updated(k, v)
    }

  import QsoGrafanaEndpoints.*

  val recentSe: ServerEndpoint[Any, IO] =
    recent.serverLogicSuccess { limit =>
        view.snapshotAll.map { all =>
          // newest first; cap for safety
          val items = all.sortBy(_.stamp).reverse.take(limit.min(5000)).toList
          RecentResponse(items)
        }
      }

  val timeseriesSe: ServerEndpoint[Any, IO] =
    timeseries.serverLogicSuccess { case (from, to, stepSeconds, groupCsv) =>
      val keys = parseGroups(groupCsv) match
        case Nil => List("band", "cabMode", "section")
        case ks  => ks

      view.snapshotAll.map { all =>
        val filtered = all.iterator.filter(q => !q.stamp.isBefore(from) && q.stamp.isBefore(to))

        val grouped =
          filtered.foldLeft(Map.empty[Map[String, String], Map[Instant, Long]]) { (acc, q) =>
            val metric = metricKey(q, keys)
            val bucket = bucketStart(q.stamp, stepSeconds)
            val byBucket = acc.getOrElse(metric, Map.empty)
            acc.updated(metric, byBucket.updated(bucket, byBucket.getOrElse(bucket, 0L) + 1L))
          }

        val series = grouped.toList.map { case (metric, buckets) =>
          Series(metric, buckets.toList.sortBy(_._1))
        }

        TimeseriesResponse(series)
      }
    }

  val geomapSe: ServerEndpoint[Any, IO] =
    geomapSections.serverLogicSuccess { case (from, to) =>
      view.snapshotAll.map { all =>
        val counts = all.iterator
          .filter(q => !q.stamp.isBefore(from) && q.stamp.isBefore(to))
          .map(_.section)
          .toList
          .groupMapReduce(identity)(_ => 1L)(_ + _)

        val points = counts.toList.flatMap { case (sec, n) =>
          sectionCentroids.get(sec).map { case (lat, lon) =>
            SectionPoint(sec, lat, lon, n)
          }
        }.sortBy(p => -p.count)

        GeomapResponse(points)
      }
    }
  override val endpoints = List(recentSe, timeseriesSe, geomapSe)
  logger.info("QsoGrafanaApi initialized with endpoints: {}", endpoints.map(_.toString).mkString("\n"))


