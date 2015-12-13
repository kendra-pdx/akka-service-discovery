package me.enkode.akka.service.discovery.core

import org.scalatest.FlatSpec

class ServiceSpec extends FlatSpec {
  behavior of "service description"

  it should "allow a service to be described" in {
    val backend = Service("backend")
    Instance("backend-a", backend, Access(Scheme.http, Host.local, 8080))
    Instance("backend-b", backend, Access(Scheme.http, Host.local, 8081))

    val frontend = Service("www")
    Instance("frontend", frontend, Access(Scheme.https, Host.local, 80))
  }
}
