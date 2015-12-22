package me.enkode.akka.service.discovery.cluster

import scala.concurrent.duration.FiniteDuration

case class ClusterServiceDiscoveryConfig(
  timeouts: ClusterServiceDiscoveryConfigTimeouts
)

case class ClusterServiceDiscoveryConfigTimeouts(
  dDataAsk: FiniteDuration
)
