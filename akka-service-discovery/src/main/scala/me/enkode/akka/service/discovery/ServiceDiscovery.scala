package me.enkode.akka.service.discovery

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import me.enkode.akka.service.discovery.core._

object ServiceDiscovery extends ExtensionId[ServiceDiscovery]{
  override def createExtension(system: ExtendedActorSystem): ServiceDiscovery = {
    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._
    val config = system.settings.config.as[ServiceDiscoveryConfig]("service-discovery")

    val localInstance: Instance = ???

    Class.forName(config.factoryClass).newInstance().asInstanceOf[ServiceDiscoveryFactory](localInstance, config, system)
  }

  trait Self {
    def shutdown(): Unit
    def start(): Unit
    def setStatus(status: Status): Unit
  }

  trait Service {
    def instances(): Future[Set[Instance]]

    def best(): Future[Option[Instance]]
    def nearest(): Future[Option[Instance]]

    def observation(instance: Instance, status: Status, latency: FiniteDuration): Unit
  }
}

trait ServiceDiscovery extends Extension {
  def self(): ServiceDiscovery.Self
  def service(id: ServiceId): ServiceDiscovery.Service
}
