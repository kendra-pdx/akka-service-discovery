package me.enkode.akka.service.discovery.cluster

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.core.Instance
import me.enkode.akka.service.discovery.{ServiceDiscoveryConfig, ServiceDiscovery, ServiceDiscoveryFactory}

class ClusterServiceDiscoveryFactory extends ServiceDiscoveryFactory {
  override def apply(localInstance: Instance, serviceDiscoveryConfig: ServiceDiscoveryConfig, system: ExtendedActorSystem): ServiceDiscovery = {
    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._

    val config = system.settings.config.getConfig("service-discovery")
      .as[ClusterServiceDiscoveryConfig]("cluster")

    new ClusterServiceDiscovery(localInstance, serviceDiscoveryConfig, config)(system) {
      if (serviceDiscoveryConfig.autoStart) {
        self().start()
      }
    }
  }
}
