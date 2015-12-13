package me.enkode.akka.service.discovery

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.core.Instance

trait ServiceDiscoveryFactory {
  def apply(
    localInstance: Instance,
    config: ServiceDiscoveryConfig,
    system: ExtendedActorSystem): ServiceDiscovery
}