package me.enkode.akka.service.discovery.core

import org.scalatest.FlatSpec

class NumericPairExtSpec extends FlatSpec {
  behavior of "numeric ranges"

  it should "determine if a number is between two numbers" in {
    assert(0 → 3 contains 2)
    assert(0 → 3 contains 0)
    assert(0 → 3 contains 3)
    assert(0f → 1f contains 0.5f)
  }

  it should "determine if a number is not between two numbers" in {
    assert(! (0 → 3 contains -1))
    assert(! (0 → 3 contains 4))
    assert(! (0f → 1f contains 1.1f))
  }

}
