package me.enkode.akka.service.discovery.cluster

import akka.actor.{ActorRef, Cancellable, ExtendedActorSystem}
import akka.event.Logging
import me.enkode.akka.service.discovery.ServiceDiscovery.Self
import me.enkode.akka.service.discovery.core._
import me.enkode.akka.service.discovery.{InstanceLoad, ServiceDiscovery, ServiceDiscoveryConfig}

import scala.concurrent.Future
import scala.concurrent.duration._

class ClusterServiceDiscovery(
  localInstance: Instance,
  serviceDiscoveryConfig: ServiceDiscoveryConfig,
  clusterServiceDiscoveryConfig: ClusterServiceDiscoveryConfig)
  (implicit actorSystem: ExtendedActorSystem) extends ServiceDiscovery {
  import actorSystem.dispatcher
  val logger = Logging.getLogger(actorSystem, this)

  def createClusterServiceDiscoveryActorRef(): ActorRef = {
    actorSystem.actorOf(ClusterServiceDiscoveryActor.props)
  }

  val sdActorRef = createClusterServiceDiscoveryActorRef()

  if (serviceDiscoveryConfig.autoStart) {
    self().start()
  }

  private var tasks: Seq[Cancellable] = Nil

  override def self(): ServiceDiscovery.Self = new Self {

    private var reportStatus: Status = Status.ok

    override def setStatus(status: Status): Unit = reportStatus = status

    def heartbeat(): Unit = {
      logger.debug("<3")
      val report = Heartbeat(localInstance, InstanceLoad.memory(), InstanceLoad.cpu(), status = reportStatus)
      sdActorRef ! ClusterServiceDiscoveryActor.SendReport(report)
    }

    def prune(): Unit = {
      logger.debug("prune…")
    }

    def startHeartbeating(): Cancellable = {
      actorSystem.scheduler.schedule(1.second, serviceDiscoveryConfig.heartbeat.frequency)(heartbeat())
    }

    def startPruning(): Cancellable = actorSystem.scheduler.schedule(1.second, 2.minute)(prune())

    override def start(): Unit = tasks.synchronized {
      tasks = startHeartbeating() :: startPruning() :: Nil
    }

    override def shutdown(): Unit = tasks.synchronized {
      tasks foreach { _.cancel() }
      tasks = Nil
    }
  }

  override def service(id: ServiceId): ServiceDiscovery.Service = new ServiceDiscovery.Service {

    override def observation(instance: Instance, status: Status, latency: FiniteDuration): Unit = {
      val observation = Observation(instance, localInstance, latency, status = status)
      sdActorRef ! ClusterServiceDiscoveryActor.SendReport(observation)
    }

    override def instances(): Future[Set[Instance]] = {
      Future(Set.empty)
    }

    override def nearest(): Future[Option[Instance]] = instances() map { _.headOption }

    override def best(): Future[Option[Instance]] = nearest()
  }
}
