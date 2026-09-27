package com.alecdorrington.dike

import munit.FunSuite

/** Tests of the ladder of anchors, and of the search that climbs it. */
final class LadderSuite extends FunSuite:

  import LadderSuite.*

  test("Beating a rung leaves only the rungs above it to search."):
    val left = Tournament.narrowed(0 until 7, 3, Some(Verdict.First))
    assertEquals(left.toList, List(4, 5, 6))

  test("Losing to a rung leaves only the rungs below it to search."):
    val left = Tournament.narrowed(0 until 7, 3, Some(Verdict.Second))
    assertEquals(left.toList, List(0, 1, 2))

  test("A drawn or contradictory verdict ends the climb."):
    assert(Tournament.narrowed(0 until 7, 3, Some(Verdict.Tie)).isEmpty)
    assert(Tournament.narrowed(0 until 7, 3, None).isEmpty)

  test("A climb narrows to nothing at the ends of the ladder."):
    val only = 0 until 1
    assert(Tournament.narrowed(only, 0, Some(Verdict.First)).isEmpty)
    assert(Tournament.narrowed(only, 0, Some(Verdict.Second)).isEmpty)

  test("A climb searches within the range it is left, never beyond it."):
    val left = Tournament.narrowed(2 until 5, 3, Some(Verdict.Second))
    assertEquals(left.toList, List(2))

  test("Each anchor is paired with the one scored next above it."):
    assertEquals(
      field(2, 40, 60, 80).rungs,
      List((2, 3), (3, 4)),
    )

  test("The ladder is climbed in score order, however it is given."):
    assertEquals(
      field(1, 80, 40, 60).ladder.map(_.score),
      Vector(40.0, 60.0, 80.0),
    )

  test("A ladder of one rung has nothing to pair it with."):
    assertEquals(field(2, 40).rungs, Nil)
    assertEquals(field(2).rungs, Nil)

  test("The anchors stand after the items, up the ladder."):
    assertEquals(
      field(2, 80, 40).players,
      Vector("0", "1", "Scored 40.0", "Scored 80.0"),
    )

  test("Rounds beyond those exhausting every pairing are not scheduled."):
    assertEquals(
      Field(Contest(
        List.tabulate(4)(_.toString),
        rounds = 99,
      )).peerings.size,
      6,
    )

object LadderSuite:

  /** The players of a contest of items named after their index. */
  private def field(items: Int, scores: Double*): Field[String] = Field(Contest(
    List.tabulate(items)(_.toString),
    scores.map(score => Anchor(s"Scored $score", score)),
  ))
