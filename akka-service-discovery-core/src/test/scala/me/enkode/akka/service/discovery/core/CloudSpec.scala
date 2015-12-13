package me.enkode.akka.service.discovery.core

import org.scalatest.{Matchers, FlatSpec}

class CloudSpec extends FlatSpec with Matchers {

  behavior of "AWS"

  it should "be able to look up AZs by id" in {
    Cloud.AWS.AZ.fromId("us-west-2a") should have (
      'region (Cloud.AWS.Region.usWest2),
      'az ('a')
    )
  }
}
