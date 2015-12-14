package me.enkode.akka.service.discovery.cluster

import akka.actor.{ActorRef, Cancellable, ExtendedActorSystem}
import akka.event.Logging
import akka.util.Timeout
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
      logger.debug("prune… i don't know how to do this yet")
    }

    def startHeartbeating(): Cancellable = {
      actorSystem.scheduler.schedule(1.second, serviceDiscoveryConfig.heartbeat.frequency)(heartbeat())
    }

    def startPruning(): Cancellable = {
      actorSystem.scheduler.schedule(1.second, 2.minute)(prune())
    }

    override def start(): Unit = synchronized {
      tasks = startHeartbeating() :: startPruning() :: Nil
    }

    override def shutdown(): Unit = synchronized {
      tasks foreach { _.cancel() }
      tasks = Nil
    }
  }

  override def service(serviceId: ServiceId): ServiceDiscovery.Service = new ServiceDiscovery.Service {

    implicit object ReportOrdering extends Ordering[Report] {
      override def compare(x: Report, y: Report): Int = {
        x.when.toEpochMilli.compareTo(y.when.toEpochMilli)
      }
    }

    override def observation(instance: Instance, status: Status, latency: FiniteDuration): Unit = {
      val observation = Observation(instance, localInstance, latency, status = status)
      sdActorRef ! ClusterServiceDiscoveryActor.SendReport(observation)
    }

    override def reports(): Future[Set[Report]] = {
      import akka.pattern.ask
      implicit val timeout = Timeout(clusterServiceDiscoveryConfig.timeouts.queryInstancesByService)
      logger.info(s"searching for reports: serviceId=$serviceId")
      sdActorRef
        .ask(ClusterServiceDiscoveryActor.GetServiceReports(serviceId))
        .mapTo[ClusterServiceDiscoveryActor.GetServiceReportsResult]
        .map(_.reports)
    }

    override def nearest(): Future[Option[Instance]] = reports() map { _.toList.sorted.headOption.map(_.instance) }

    override def best(): Future[Option[Instance]] = nearest()
  }
}
