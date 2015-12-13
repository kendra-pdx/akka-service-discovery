package me.enkode.akka.service.discovery

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}

import scala.concurrent.duration.FiniteDuration
import me.enkode.akka.service.discovery.core._

object ServiceDiscovery extends ExtensionId[ServiceDiscovery]{
  override def createExtension(system: ExtendedActorSystem): ServiceDiscovery = {
    val factoryClass = system.settings.config.getString("service-discovery.factory")
    Class.forName(factoryClass).newInstance().asInstanceOf[ServiceDiscoveryFactory](system)
  }

  trait Self {
    def shutdown(): Unit
    def start(): Unit
    def setStatus(): Unit
  }

  trait Service {
    def instances(): Seq[Instance]
    def best(): Option[Instance]
    def nearest(): Option[Instance]

    def report(status: Status, latency: FiniteDuration): Unit
  }
}

trait ServiceDiscovery extends Extension {
  def self(): ServiceDiscovery.Self
  def service(id: ServiceId): ServiceDiscovery.Service
}
