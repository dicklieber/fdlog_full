package fdswarm.metric

import fdswarm.logging.Locus
import fdswarm.util.NodeIdentity

/** Builds a metric name. Usually each step is curried
  * @param nodeIdentity
  * @param locus
  * @param direction
  * @param name
  */
class MetricNameBuilder(nodeIdentity: NodeIdentity)(locus: Locus)(direction: Direction)(name: String):
  override val toString: String =
    s"${nodeIdentity.external}.${locus.toString}.$name.${direction.toString}"

object MetricNameBuilder:
  def apply(nodeIdentity: NodeIdentity)(locus: Locus)(direction: Direction)(name: String)
      : MetricNameBuilder =
    new MetricNameBuilder(nodeIdentity)(locus)(direction)(name)

  def forNodeAndLocus(
      nodeIdentity: NodeIdentity,
      locus: Locus
  ): Direction => String => MetricNameBuilder =
    MetricNameBuilder(nodeIdentity)(locus)

  given Conversion[MetricNameBuilder, String] = _.toString

enum Direction:
  case Send, Received
