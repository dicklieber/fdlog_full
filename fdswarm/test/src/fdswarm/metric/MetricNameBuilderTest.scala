package fdswarm.metric

import fdswarm.logging.Locus
import fdswarm.metric.MetricName.Direction
import fdswarm.util.NodeIdentity
import munit.FunSuite

class MetricNameBuilderTest extends FunSuite:

  test("metricNameBuilder"):
    val nodeIdentity = NodeIdentity.mockNodeIdentity
    val fromNodeIdentity = MetricNameBuilder(nodeIdentity)
    val fromLocus = fromNodeIdentity(Locus.Replication)
    val fromDirection = fromLocus(Direction.Send)
    val metricNameBuilder = fromDirection("testMetric")
    val metricName = metricNameBuilder.toString
    assertEquals(metricName,  "testHost:=id.Replication.testMetric.In")
