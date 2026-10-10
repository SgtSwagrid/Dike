package com.alecdorrington.dike

import com.alecdorrington.dike.FieldSuite.*
import munit.FunSuite

final class FieldSuite extends FunSuite:

  test("each anchor is paired with the one scored next above it"):
    assertEquals(
      field(2, 40, 60, 80).rungs,
      List((2, 3), (3, 4)),
    )

  test("the ladder is climbed in score order, however it is given"):
    assertEquals(
      field(1, 80, 40, 60).ladder.map(_.score),
      Vector(40.0, 60.0, 80.0),
    )

  test("a ladder of one rung has nothing to pair it with"):
    assertEquals(field(2, 40).rungs, Nil)
    assertEquals(field(2).rungs, Nil)

  test("the anchors stand after the items, up the ladder"):
    assertEquals(
      field(2, 80, 40).players,
      Vector("0", "1", "Scored 40.0", "Scored 80.0"),
    )

  test("rounds beyond those exhausting every pairing are not scheduled"):
    assertEquals(
      Field(Contest(
        List.tabulate(4)(_.toString),
        rounds = 99,
      )).peerings.size,
      6,
    )

  test("a climb is compared with at most one rung per halving of the ladder"):
    assertEquals(
      List(0, 1, 2, 3, 7, 8, 15, 16).map(Field.longestClimb),
      List(0, 1, 2, 2, 3, 4, 4, 5),
    )

object FieldSuite:

  private def field(items: Int, scores: Double*): Field[String] = Field(Contest(
    List.tabulate(items)(_.toString),
    scores.map(score => Anchor(s"Scored $score", score)),
  ))
