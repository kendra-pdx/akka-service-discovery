package me.enkode.akka.service.discovery

import akka.actor.ActorSystem

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

package object cluster {
  def retryUntil[Result](max: Int = 20, delay: FiniteDuration = 1.second)
    (future: ⇒ Future[Result])
    (until: Result ⇒ Boolean)
    (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Result] = {
    val promise = Promise[Result]()

    def nextAttempt(attemptNo: Int = 0): Unit ={
      future onComplete {
        case Success(r) if until(r) ⇒
          promise.success(r)

        case Failure(_) | Success(_) if attemptNo < max ⇒
          actorSystem.scheduler.scheduleOnce(delay) {
            nextAttempt(attemptNo + 1)
          }

        case Failure(t) ⇒
          promise.failure(t)

        case result ⇒
          promise.failure(new RuntimeException(s"no more attempts: max=$max, $result"))
      }
    }

    nextAttempt()

    promise.future
  }
}
