package com.alecdorrington.dike

import munit.FunSuite

final class JudgingSuite extends FunSuite:

  test("beating a rung leaves only the rungs above it to search"):
    val left = Judging.narrowed(0 until 7, 3, Some(Verdict.First))
    assertEquals(left.toList, List(4, 5, 6))

  test("losing to a rung leaves only the rungs below it to search"):
    val left = Judging.narrowed(0 until 7, 3, Some(Verdict.Second))
    assertEquals(left.toList, List(0, 1, 2))

  test("a drawn or contradictory verdict ends the climb"):
    assert(Judging.narrowed(0 until 7, 3, Some(Verdict.Tie)).isEmpty)
    assert(Judging.narrowed(0 until 7, 3, None).isEmpty)

  test("a climb narrows to nothing at the ends of the ladder"):
    val only = 0 until 1
    assert(Judging.narrowed(only, 0, Some(Verdict.First)).isEmpty)
    assert(Judging.narrowed(only, 0, Some(Verdict.Second)).isEmpty)

  test("a climb searches within the range it is left, never beyond it"):
    val left = Judging.narrowed(2 until 5, 3, Some(Verdict.Second))
    assertEquals(left.toList, List(2))
