package me.enkode.akka.service.discovery

import scala.concurrent.duration.FiniteDuration

case class ServiceDiscoveryConfig(
  autoStart: Boolean,
  factoryClass: String,
  service: ServiceDiscoveryServiceConfig,
  heartbeat: ServiceDiscoveryHeartbeatConfig,
  cloud: ServiceDiscoveryCloudConfig)

case class ServiceDiscoveryServiceConfig(
  serviceId: String
)

case class ServiceDiscoveryHeartbeatConfig(
  frequency: FiniteDuration
)

case class ServiceDiscoveryCloudConfig()
