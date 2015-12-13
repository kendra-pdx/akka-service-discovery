package me.enkode.akka.service.discovery

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

package object core {
  type ServiceId = String
  type InstanceId = String

  implicit class InstantExt(val instant: Instant) extends AnyVal {
    def - (fd: FiniteDuration): Instant = {
      instant.minusMillis(fd.toMillis)
    }

    def + (fd: FiniteDuration): Instant = {
      instant.plusMillis(fd.toMillis)
    }
  }

  implicit class NumericPairExt[T: Numeric](val pair: (T, T)) {
    val numeric = implicitly[Numeric[T]]
    def contains(value: T): Boolean = {
      val (a, b) = pair
      require(numeric.lteq(a, b), "a must be ≤ b")
      numeric.lteq(a, value) && numeric.gteq(b, value)
    }
  }
}
