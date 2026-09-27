package com.alecdorrington.dike

import munit.FunSuite

/** Tests of the round-robin schedule that comparisons are drawn from. */
final class RoundRobinSuite extends FunSuite:

  test("A full round-robin plays every pairing exactly once."):
    val played = RoundRobin.pairings(6, 5).map((a, b) => (a.min(b), a.max(b)))
    assertEquals(played.size, 15)
    assertEquals(played.distinct.size, 15)

  test("Rounds beyond a full round-robin are not scheduled."):
    assertEquals(
      RoundRobin.pairings(4, 99).toList,
      RoundRobin.pairings(4, 3).toList,
    )

  test("Every player is paired exactly once per round."):
    val round = RoundRobin.pairings(8, 1)
    assertEquals(round.size, 4)
    val seated = round.flatMap((a, b) => Seq(a, b)).sorted.toList
    assertEquals(seated, (0 until 8).toList)

  test("An odd field sits one player out of each round."):
    val round = RoundRobin.pairings(5, 1)
    assertEquals(round.size, 2)
    assertEquals(
      round.flatMap((a, b) => Seq(a, b)).distinct.size,
      4,
    )

  test("An odd field still plays every pairing over a full round-robin."):
    val played = RoundRobin.pairings(5, 5).map((a, b) => (a.min(b), a.max(b)))
    assertEquals(played.distinct.size, 10)

  test("Every player is compared equally often over several rounds."):
    val appearances = RoundRobin
      .pairings(6, 3)
      .flatMap((a, b) => Seq(a, b))
      .groupMapReduce(identity)(_ => 1)(_ + _)
    assertEquals(appearances.values.toSet, Set(3))

  test("A field too small to pair is not scheduled."):
    assert(RoundRobin.pairings(1, 3).isEmpty)
    assert(RoundRobin.pairings(0, 3).isEmpty)

  test("No rounds are scheduled when none are asked for."):
    assert(RoundRobin.pairings(4, 0).isEmpty)
