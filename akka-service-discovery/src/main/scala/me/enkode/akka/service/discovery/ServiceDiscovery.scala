package me.enkode.akka.service.discovery

import java.lang.management.ManagementFactory

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import me.enkode.akka.service.discovery.core._

object ServiceDiscovery extends ExtensionId[ServiceDiscovery]{
  override def createExtension(system: ExtendedActorSystem): ServiceDiscovery = {
    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._
    val config = system.settings.config.as[ServiceDiscoveryConfig]("service-discovery")
    val pid = ManagementFactory.getRuntimeMXBean().getName()
    val localInstance: Instance = Instance(pid, Service("myService"), Access(Scheme.http, Host.local, 8080))

    Class.forName(config.factoryClass).newInstance().asInstanceOf[ServiceDiscoveryFactory](localInstance, config, system)
  }

  trait Self {
    def shutdown(): Unit
    def start(): Unit
    def setStatus(status: Status): Unit
  }

  trait Service {
    def reports(): Future[Set[Report]]

    def best(): Future[Option[Instance]]
    def nearest(): Future[Option[Instance]]

    def observation(instance: Instance, status: Status, latency: FiniteDuration): Unit
  }
}

trait ServiceDiscovery extends Extension {
  def self(): ServiceDiscovery.Self
  def service(id: ServiceId): ServiceDiscovery.Service
}
