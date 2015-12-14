package me.enkode.akka.service.discovery.cluster

import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.{Config, ConfigFactory}
import me.enkode.akka.service.discovery.ServiceDiscovery
import me.enkode.akka.service.discovery.core.Report
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent._, duration._

class ClusterServiceDiscoverySpecMultiJvmNode1 extends ClusterServiceDiscoverySpec
class ClusterServiceDiscoverySpecMultiJvmNode2 extends ClusterServiceDiscoverySpec
class ClusterServiceDiscoverySpecMultiJvmNode3 extends ClusterServiceDiscoverySpec

object ClusterServiceDiscoverySpec extends MultiNodeConfig {
  val node1 = role("node-1")
  val node2 = role("node-2")
  val node3 = role("node-3")

  commonConfig {
    ConfigFactory.parseString(
      """
        |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
        |akka.loglevel = DEBUG
        |akka.actor {
        |  provider = "akka.cluster.ClusterActorRefProvider"
        |  debug {
        |   log-received-messages = on
        |   unhandled = on
        |  }
        |}
        |akka.log-dead-letters-during-shutdown = false
        |akka.remote {
        |  netty.tcp.hostname = "127.0.0.1"
        |}
        |akka.cluster {
        |  seed-nodes = [
        |    "akka.tcp://ClusterServiceDiscoverySpec@127.0.0.1:2551",
        |    "akka.tcp://ClusterServiceDiscoverySpec@127.0.0.1:2552",
        |    "akka.tcp://ClusterServiceDiscoverySpec@127.0.0.1:2553" ]
        |}
      """.stripMargin)
      .withFallback(ConfigFactory.load())
  }

  def nodeConfig(port: Int) = ConfigFactory.parseString(
    s"""
      |akka.remote {
      |  netty.tcp.port = $port
      |}
      |""".stripMargin)

  nodeConfig(node1)(nodeConfig(2551))
  nodeConfig(node2)(nodeConfig(2552))
  nodeConfig(node3)(nodeConfig(2553))
}

class ClusterServiceDiscoverySpec extends MultiNodeSpec(ClusterServiceDiscoverySpec)
  with FlatSpecLike with BeforeAndAfterAll with ImplicitSender with ScalaFutures {
  override def initialParticipants: Int = roles.size
  import system.dispatcher
  implicit val patience = PatienceConfig(Span(10, Seconds), Span(200, Millis))

  enterBarrier("starting up")

  val discovery = ServiceDiscovery(system)

  behavior of "finding reports"

  it should "find a report from each node" in {
    enterBarrier("find reports")

    val promisedReports = Promise[Set[Report]]()
    var attempts = 0

    def makeAttempt(): Unit = {
      attempts += 1
      if (attempts > 10) promisedReports.failure(new Exception("attempts exceeded"))
      system.scheduler.scheduleOnce(1.second) {
        discovery.service("myService").reports() map { reports ⇒
          if (reports.size < 3) {
            makeAttempt()
          } else {
            promisedReports.success(reports)
          }
        }
      }
    }

    makeAttempt()
    whenReady(promisedReports.future) { reports ⇒
      println(s"FOUND: $reports")
    }

    enterBarrier("found reports… or not")
  }

  enterBarrier("stopping")

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
