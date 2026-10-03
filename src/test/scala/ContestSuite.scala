package com.alecdorrington.dike

import cats.effect.{IO, Ref}
import com.alecdorrington.dike.ContestSuite.*
import munit.CatsEffectSuite

final class ContestSuite extends CatsEffectSuite:

  test("items are ranked in the order a consistent judge prefers them"):
    Contest(List(3, 1, 4, 2, 5), rounds = 5)
      .rank(fair)
      .map(ranking =>
        assertEquals(
          ranking.ranked.map(_.item),
          Vector(5, 4, 3, 2, 1),
        ),
      )

  test("every pairing is judged both ways round"):
    for
      heard  <- Ref.of[IO, List[(Int, Int)]](Nil)
      _      <- Contest(List(1, 2)).rank(recording(heard))
      orders <- heard.get
    yield assertEquals(orders.toSet, Set((1, 2), (2, 1)))

  test("a judge that always prefers what it sees first favours no item"):
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

  test("items placed on the ladder are scored among the anchors they meet"):
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

  test("the ladder is climbed by binary search"):
    // Four of the fifteen rungs are searched, and the fourteen adjacent pairs
    // compared, each judged twice.
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

  test("the longest climbs call for as many judgements as a contest may"):
    // Every item loses to every anchor, so climbs all the way down.
    val contest = Contest(
      List(1, 2, 3),
      ladder((10 to 150 by 10)*),
      rounds = 2,
    )
    for
      heard  <- Ref.of[IO, List[(Int, Int)]](Nil)
      _      <- contest.rank(recording(heard))
      orders <- heard.get
    yield
      assertEquals(contest.maxJudgements, 56)
      assertEquals(orders.size, contest.maxJudgements)

  test("a draw against a rung ends the climb"):
    Contest(
      List(80),
      ladder((10 to 150 by 10)*),
      rounds = 0,
    ).rank(fair).map(ranking => assertEquals(ranking.judgements, 2 + 28))

  test("every climb begins on the middle rung, which is presented first most"):
    assertEquals(
      Contest(List(1, 2, 3), ladder(10, 20, 30), rounds = 0).presentedFirst,
      // Each item's first rung, and the two rungs either side of it.
      Map(1 -> 1, 2 -> 1, 3 -> 1, 10 -> 1, 20 -> (3 + 2), 30 -> 1),
    )

  test("a judge that always ties is asked only what is sure to be judged"):
    // A draw ends every climb on its first rung, leaving the judgements made
    // whatever the verdicts.
    val contest = Contest(
      List(1, 2, 3, 4, 5),
      ladder(10, 20, 30, 40),
      rounds = 2,
    )
    for
      heard <- Ref.of[IO, List[(Int, Int)]](Nil)
      _     <- contest.rank((first, second) =>
        heard.update((first, second) :: _).as(Verdict.Tie),
      )
      orders <- heard.get
    yield assertEquals(
      orders.groupMapReduce(_._1)(_ => 1)(_ + _),
      contest.presentedFirst,
    )

  test("a judge's failures are reported, and the ranking fitted without them"):
    val flaky: Judge[IO, Int] = (first, second) =>
      if first == 3 || second == 3 then IO.raiseError(RuntimeException("3"))
      else fair(first, second)
    Contest(List(1, 2, 3), rounds = 3)
      .rank(flaky)
      .map: ranking =>
        assertEquals(ranking.failures.size, 4)
        assertEquals(ranking.judgements, 2)
        assertEquals(ranking.ranked.head.item, 2)

  test("a judge that throws fails its judgement, never the contest"):
    val broken: Judge[IO, Int] = (_, _) => throw RuntimeException("Broken.")
    Contest(List(1, 2, 3), rounds = 3)
      .rank(broken)
      .map: ranking =>
        assertEquals(ranking.failures.size, 6)
        assertEquals(ranking.judgements, 0)

  test("a contest with nothing to compare rates every item level"):
    Contest(List(1, 2), rounds = 0)
      .rank(fair)
      .map: ranking =>
        assertEquals(ranking.judgements, 0)
        assertEquals(
          ranking.ratings.map(_.elo).distinct.size,
          1,
        )

object ContestSuite:

  private val fair: Judge[IO, Int] = (first, second) =>
    IO.pure(
      if first > second then Verdict.First
      else if first < second then Verdict.Second
      else Verdict.Tie,
    )

  private val firstSeen: Judge[IO, Int] = (_, _) => IO.pure(Verdict.First)

  private def recording(heard: Ref[IO, List[(Int, Int)]]): Judge[IO, Int] =
    (first, second) => heard.update((first, second) :: _) *> fair(first, second)

  /** Anchors of the given numbers, each scored a tenth of its value. */
  private def ladder(values: Int*): List[Anchor[Int]] = values
    .toList
    .map(value => Anchor(value, value / 10.0))
