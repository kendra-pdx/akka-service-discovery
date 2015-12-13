package me.enkode.akka.service.discovery.eureka

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.{ServiceDiscovery, ServiceDiscoveryFactory}

class EurekaServiceDiscoveryFactory extends ServiceDiscoveryFactory {
  override def apply(system: ExtendedActorSystem): ServiceDiscovery = new EurekaServiceDiscovery
}
