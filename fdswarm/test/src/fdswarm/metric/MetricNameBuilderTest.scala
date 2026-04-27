package fdswarm.metric

import fdswarm.logging.Locus
import fdswarm.metric.Direction
import fdswarm.util.NodeIdentity
import munit.FunSuite

class MetricNameBuilderTest extends FunSuite:

  test("metricNameBuilder"):
    val nodeIdentity = NodeIdentity.mockNodeIdentity
    val fromNodeIdentity = MetricNameBuilder(nodeIdentity)
    val fromLocus = fromNodeIdentity(Locus.Replication)
    val fromDirection = fromLocus(Direction.Send)
    val metricNameBuilder = fromDirection("testMetric")
    val metricName:String = metricNameBuilder // testing given conversion toString
    assertEquals(metricName,  "testHost:=id.Replication.testMetric.Send")
