package me.enkode.akka.service.discovery.cluster

import akka.actor.ExtendedActorSystem
import me.enkode.akka.service.discovery.ServiceDiscovery
import me.enkode.akka.service.discovery.core._

class ClusterServiceDiscovery(implicit actorSystem: ExtendedActorSystem) extends ServiceDiscovery {
  val config = actorSystem.settings.config.getConfig("service-discovery")

  if (config.getBoolean("auto-start")) {
    self().start()
  }

  override def self(): ServiceDiscovery.Self = ???

  override def service(id: ServiceId): ServiceDiscovery.Service = ???
}
