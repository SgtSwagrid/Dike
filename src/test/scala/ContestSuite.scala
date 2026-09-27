package com.alecdorrington.dike

import cats.effect.{IO, Ref}
import munit.CatsEffectSuite

/** Tests of whole contests, judged by judges of known taste. */
final class ContestSuite extends CatsEffectSuite:

  import ContestSuite.*

  test("Items are ranked in the order a consistent judge prefers them."):
    Contest(List(3, 1, 4, 2, 5), rounds = 5)
      .rank(fair)
      .map(ranking =>
        assertEquals(
          ranking.ranked.map(_.item),
          Vector(5, 4, 3, 2, 1),
        ),
      )

  test("Every pairing is judged both ways round."):
    for
      heard  <- Ref.of[IO, List[(Int, Int)]](Nil)
      _      <- Contest(List(1, 2)).rank(recording(heard))
      orders <- heard.get
    yield assertEquals(orders.toSet, Set((1, 2), (2, 1)))

  test("A judge that always prefers what it sees first favours no item."):
    Contest(List(1, 2, 3, 4), rounds = 3)
      .rank(firstSeen)
      .map(ranking =>
        assertEquals(
          ranking.ratings.map(_.wins),
          Vector.fill(4)(1.5),
        )
        ranking
          .ratings
          .foreach(rating => assertEqualsDouble(rating.elo, 1500.0, 1e-6)),
      )

  test("Items placed on the ladder are scored among the anchors they meet."):
    Contest(
      List(25, 35),
      ladder(10, 20, 30, 40),
      rounds = 0,
    ).rank(fair)
      .map: ranking =>
        val lower = ranking.ratings(0).score
        val upper = ranking.ratings(1).score
        assert(lower > 2 && lower < 3, lower)
        assert(upper > 3 && upper < 4, upper)

  test("The ladder is climbed by binary search."):
    // Fifteen rungs take four comparisons to search, each judged twice, and
    // the fourteen pairings of adjacent rungs are judged twice besides.
    for
      heard   <- Ref.of[IO, List[(Int, Int)]](Nil)
      ranking <- Contest(
        List(55),
        ladder((10 to 150 by 10)*),
        rounds = 0,
      ).rank(recording(heard))
      orders <- heard.get
    yield
      assertEquals(orders.size, 36)
      assertEquals(ranking.judgements, 36)

  test("A draw against a rung ends the climb."):
    Contest(
      List(80),
      ladder((10 to 150 by 10)*),
      rounds = 0,
    ).rank(fair).map(ranking => assertEquals(ranking.judgements, 2 + 28))

  test("A judge's failures are reported, and the ranking fitted without them."):
    val flaky: Judge[IO, Int] = (first, second) =>
      if first == 3 || second == 3 then IO.raiseError(RuntimeException("3"))
      else fair(first, second)
    Contest(List(1, 2, 3), rounds = 3)
      .rank(flaky)
      .map: ranking =>
        assertEquals(ranking.failures.size, 4)
        assertEquals(ranking.judgements, 2)
        assertEquals(ranking.ranked.head.item, 2)

  test("A judge that throws fails its judgement, never the contest."):
    val broken: Judge[IO, Int] = (_, _) => throw RuntimeException("Broken.")
    Contest(List(1, 2, 3), rounds = 3)
      .rank(broken)
      .map: ranking =>
        assertEquals(ranking.failures.size, 6)
        assertEquals(ranking.judgements, 0)

  test("A contest with nothing to compare rates every item level."):
    Contest(List(1, 2), rounds = 0)
      .rank(fair)
      .map: ranking =>
        assertEquals(ranking.judgements, 0)
        assertEquals(
          ranking.ratings.map(_.elo).distinct.size,
          1,
        )

object ContestSuite:

  /** A judge that prefers the larger number, and ties equal ones. */
  private val fair: Judge[IO, Int] = (first, second) =>
    IO.pure(
      if first > second then Verdict.First
      else if first < second then Verdict.Second
      else Verdict.Tie,
    )

  /** A judge that prefers whichever number it sees first. */
  private val firstSeen: Judge[IO, Int] = (_, _) => IO.pure(Verdict.First)

  /** A fair judge that notes the order it was shown each pairing in. */
  private def recording(heard: Ref[IO, List[(Int, Int)]]): Judge[IO, Int] =
    (first, second) => heard.update((first, second) :: _) *> fair(first, second)

  /** Anchors of the given numbers, each scored a tenth of its value. */
  private def ladder(values: Int*): List[Anchor[Int]] = values
    .toList
    .map(value => Anchor(value, value / 10.0))
