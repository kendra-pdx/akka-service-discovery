package me.enkode.akka.service.discovery.cluster

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, ORMap, ORMapKey, ORSet}
import akka.event.LoggingReceive
import me.enkode.akka.service.discovery.core._

object ClusterServiceDiscoveryActor {
  def props: Props = Props(new ClusterServiceDiscoveryActor)

  sealed trait Command
  sealed trait Result[C <: Command] {
    def command: C
  }

  case class SendReport(report: Report) extends Command

  case class PruneHeartbeats(instance: Instance, before: Instant) extends Command

  case class GetServiceReports(serviceId: ServiceId) extends Command
  case class GetServiceReportsResult(command: GetServiceReports, reports: Set[Report]) extends Result[GetServiceReports]
}

class ClusterServiceDiscoveryActor extends Actor with ActorLogging {
  import ClusterServiceDiscoveryActor._
  import akka.cluster.ddata.Replicator.{Command ⇒ _, _}

  val replicator = DistributedData(context.system).replicator
  implicit val cluster = Cluster(context.system)

  val rc = ReadLocal
  val wc = WriteLocal

  type ReportsByService = ORMap[ORSet[Report]]
  val reportsByService = ORMap.empty[ORSet[Report]]

  def updateReportsByService(report: Report)(byService:  ORMap[ORSet[Report]]): ORMap[ORSet[Report]] = {
    byService.updated(cluster, report.instance.service.serviceId, ORSet.empty[Report]){ _ + report }
  }

  def keyOf(serviceId: String): ORMapKey[ORSet[Report]] = ORMapKey[ORSet[Report]](serviceId)

  override def receive: Receive = LoggingReceive {
    // SEND REPORTS
    case SendReport(report) ⇒
      replicator ! Update(keyOf(report.instance.service.serviceId), reportsByService, wc, None)(updateReportsByService(report))

    // GET REPORTS

    case command@GetServiceReports(serviceId) ⇒
      log.info(s"asking the replicator for reports for $serviceId")
      val replyTo = sender()
      val key = ORMapKey[ORSet[Report]](serviceId)
      replicator ! Get(key, rc, Some(command → replyTo))

    case success@GetSuccess(ORMapKey(serviceId), Some((command: GetServiceReports, replyTo: ActorRef))) ⇒
      val data = success.get(keyOf(serviceId))
      log.info(s"get success for serviceId: $serviceId - $data")
      val reports: Set[Report] = {
        data.entries.values.map(_.elements).toSet.flatten
      }
      replyTo ! GetServiceReportsResult(command, reports)

    case NotFound(_, Some((GetServiceReports(_), replyTo: ActorRef))) ⇒
      replyTo ! Set.empty[Report]
  }
}
