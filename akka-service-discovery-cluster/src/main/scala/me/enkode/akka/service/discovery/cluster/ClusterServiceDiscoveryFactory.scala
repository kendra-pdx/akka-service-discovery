package me.enkode.akka.service.discovery.cluster

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.{ServiceDiscovery, ServiceDiscoveryFactory}

class ClusterServiceDiscoveryFactory extends ServiceDiscoveryFactory {
  override def apply(system: ExtendedActorSystem): ServiceDiscovery = new ClusterServiceDiscovery()(system)
}
