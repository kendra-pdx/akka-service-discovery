package me.enkode.akka.service.discovery.cluster

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.contrib.datareplication.{ORMap, DataReplication, ORSet}
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
  import akka.contrib.datareplication.Replicator.{Command ⇒ _, _}

  val replicator = DataReplication(context.system).replicator
  implicit val cluster = Cluster(context.system)

  val rc = ReadLocal
  val wc = WriteLocal

  type ReportsByService = ORMap[ORSet[Report]]
  val reportsByService = ORMap.empty[ORSet[Report]]

  def updateReportsByService(report: Report)(byService: ReportsByService): ReportsByService = {
    byService.updated(cluster, report.instance.service.serviceId, ORSet.empty[Report]){ _ + report }
  }

  override def receive: Receive = LoggingReceive {
    // SEND HEARTBEAT
    case SendReport(report) ⇒
      replicator ! Update(report.instance.service.serviceId, reportsByService, rc, wc, None)(updateReportsByService(report))

    // PRUNE
    case command@PruneHeartbeats(instance, _) ⇒
      replicator ! Get(instance.instanceId, rc, Some(command))

    case GetSuccess(instanceId, data: ORSet[Report] @unchecked, Some(PruneHeartbeats(instance, before))) ⇒
      data.elements filter { _.when.isBefore(before) } foreach { prune ⇒
        replicator ! Update(instanceId, data, rc, wc) { _ - prune }
      }

    case NotFound(_, Some(PruneHeartbeats(instance, before))) ⇒
      // do nothing

    // GET REPORTS

    case command@GetServiceReports(serviceId) ⇒
      log.info(s"asking the replicator for reports for $serviceId")
      val replyTo = sender()
      replicator ! Get(serviceId, rc, Some(command → replyTo))

    case GetSuccess(serviceId, data: ReportsByService @unchecked, Some((command: GetServiceReports, replyTo: ActorRef))) ⇒
      log.info(s"get success for serviceId: $serviceId - $data")
      val reports: Set[Report] = {
        data.entries.values.map(_.elements).toSet.flatten
      }
      replyTo ! GetServiceReportsResult(command, reports)

    case NotFound(_, Some((GetServiceReports(_), replyTo: ActorRef))) ⇒
      replyTo ! Set.empty[Report]
  }
}
