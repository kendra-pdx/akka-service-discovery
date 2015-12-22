package me.enkode.akka.service.discovery.cluster

import akka.actor.{Cancellable, ExtendedActorSystem}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORMap, ORMapKey, ORSet}
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
  import akka.pattern.ask

  val logger = Logging.getLogger(actorSystem, this)

  if (serviceDiscoveryConfig.autoStart) {
    self().start()
  }

  private var tasks: Seq[Cancellable] = Nil

  implicit val askTimeout = Timeout(clusterServiceDiscoveryConfig.timeouts.dDataAsk)
  implicit val cluster = Cluster(actorSystem)

  val replicator = DistributedData(actorSystem).replicator

  def keyOf(serviceId: String): ORMapKey[ORSet[Report]] = ORMapKey[ORSet[Report]](serviceId)

  def updateReportsByService(report: Report)(byService:  ORMap[ORSet[Report]]): ORMap[ORSet[Report]] = {
    byService.updated(cluster, report.instance.service.serviceId, ORSet.empty[Report]){ _ + report }
  }

  def reportUpdate(report: Report): Update[ORMap[ORSet[Report]]] = {
    val reportsByService = ORMap.empty[ORSet[Report]]
    Update(keyOf(report.instance.service.serviceId), reportsByService, WriteLocal, None)(updateReportsByService(report))
  }

  def reportsByServiceGet(serviceId: ServiceId): Get[ORMap[ORSet[Report]]] = {
    val key = ORMapKey[ORSet[Report]](serviceId)
    Get(key, ReadLocal, None)
  }

  override def self(): ServiceDiscovery.Self = new Self {

    private var reportStatus: Status = Status.ok

    override def setStatus(status: Status): Unit = reportStatus = status

    def heartbeat(): Unit = {
      logger.debug("<3")
      val report = Heartbeat(localInstance, InstanceLoad.memory(), InstanceLoad.cpu(), status = reportStatus)
      (replicator ask reportUpdate(report)) map {
        case response ⇒ logger.debug(s"replicator update response: $response")
      }
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
    def updateReportsByService(report: Report)(byService:  ORMap[ORSet[Report]]): ORMap[ORSet[Report]] = {
      byService.updated(cluster, report.instance.service.serviceId, ORSet.empty[Report]){ _ + report }
    }

    implicit object ReportOrdering extends Ordering[Report] {
      override def compare(x: Report, y: Report): Int = {
        x.when.toEpochMilli.compareTo(y.when.toEpochMilli)
      }
    }

    override def observation(instance: Instance, status: Status, latency: FiniteDuration): Unit = {
      val report = Observation(instance, localInstance, latency, status = status)
      (replicator ask reportUpdate(report)) map {
        case response ⇒ logger.debug(s"replicator update response: $response")
      }
    }

    override def reports(): Future[Set[Report]] = {
      import akka.pattern.ask
      logger.info(s"searching for reports: serviceId=$serviceId")

      (replicator ask reportsByServiceGet(serviceId)) map {
        case success@GetSuccess(ORMapKey(`serviceId`), _) ⇒
          success.get(keyOf(serviceId)).entries.values.flatMap(_.elements).toSet

        case NotFound(ORMapKey(`serviceId`), _) ⇒
          Set.empty[Report]
      }
    }

    override def nearest(): Future[Option[Instance]] = reports() map { _.toList.sorted.headOption.map(_.instance) }

    override def best(): Future[Option[Instance]] = nearest()
  }
}
