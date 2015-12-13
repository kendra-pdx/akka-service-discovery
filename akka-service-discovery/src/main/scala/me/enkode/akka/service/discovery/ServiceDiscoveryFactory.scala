package me.enkode.akka.service.discovery

import akka.actor.ExtendedActorSystem

trait ServiceDiscoveryFactory {
  def apply(system: ExtendedActorSystem): ServiceDiscovery
}
