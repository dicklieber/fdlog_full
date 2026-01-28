package fdswarm.model

import java.time.Instant

/**
 * A case clas that has a stamp and can be tested for newness.
 *
 */
trait Stamped[T <: Stamped[T]] extends Product:
  self: Product =>
  val stamp: Instant
