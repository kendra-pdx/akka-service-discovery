package me.enkode.akka.service.discovery.eureka

import me.enkode.akka.service.discovery.ServiceDiscovery
import me.enkode.akka.service.discovery.core._


class EurekaServiceDiscovery extends ServiceDiscovery {
  override def self(): ServiceDiscovery.Self = ???

  override def service(id: ServiceId): ServiceDiscovery.Service = ???
}
