package me.enkode.akka.service.discovery.eureka

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.core.Instance
import me.enkode.akka.service.discovery.{ServiceDiscoveryConfig, ServiceDiscovery, ServiceDiscoveryFactory}

class EurekaServiceDiscoveryFactory extends ServiceDiscoveryFactory {
  override def apply(localInstance: Instance, baseConfig: ServiceDiscoveryConfig, system: ExtendedActorSystem): ServiceDiscovery = {
    new EurekaServiceDiscovery
  }
}
