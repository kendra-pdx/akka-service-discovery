package me.enkode.akka.service.discovery.cluster

import java.time.Instant

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.contrib.datareplication.{DataReplication, ORSet}
import me.enkode.akka.service.discovery.core._

object ClusterServiceDiscoveryActor {
  def props: Props = Props(new ClusterServiceDiscoveryActor)

  sealed trait Command
  sealed trait Result {
    def command: Command
  }

  case class SendReport(report: Report) extends Command

  case class PruneHeartbeats(instance: Instance, before: Instant) extends Command

  case class GetReports(instanceId: InstanceId) extends Command
  case class GetReportsResult(command: GetReports, reports: Set[Report]) extends Result
}

class ClusterServiceDiscoveryActor extends Actor with ActorLogging {
  import ClusterServiceDiscoveryActor._
  import akka.contrib.datareplication.Replicator.{Command ⇒ _, _}
  val replicator = DataReplication(context.system).replicator
  implicit val cluster = Cluster(context.system)

  val rc = ReadLocal
  val wc = WriteLocal

  //todo: this is all wrong!

  override def receive: Receive = {
    // SEND HEARTBEAT
    case SendReport(report) ⇒
      replicator ! Update(
        report.instance.instanceId,
        ORSet.empty[Report], rc, wc, None) {_ + report }

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

    case command@GetReports(instanceId) ⇒
      replicator ! Get(instanceId, rc, Some(command → sender()))

    case GetSuccess(instanceId, data: ORSet[Report] @unchecked, Some((GetReports(_), replyTo: ActorRef))) ⇒
      replyTo ! data.elements

    case NotFound(_, Some((GetReports(_), replyTo: ActorRef))) ⇒
      replyTo ! Nil
  }
}
