package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import com.alecdorrington.dike.RankingSuite.*
import munit.FunSuite

final class RankingSuite extends FunSuite:

  test("the stronger of two items is ranked and scored above the weaker"):
    val ranked = rank(2, beat(0, 1)).ranked
    assertEquals(ranked.map(_.item), Vector("0", "1"))
    assert(ranked.head.score > ranked.last.score)

  test("ratings keep the order the items were given in"):
    val ranking = rank(2, beat(1, 0))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0", "1"),
    )
    assertEquals(
      ranking.ranked.map(_.item),
      Vector("1", "0"),
    )

  test("agreeing judgements amount to one comparison won"):
    assertEqualsDouble(
      rank(2, beat(0, 1)).ratings(0).wins,
      1.0,
      1e-9,
    )

  test("contradictory judgements amount to a draw"):
    val ranking = rank(
      2,
      List(
        List(Outcome(0, 1, 0.5)),
        List(Outcome(1, 0, 0.5)),
      ),
    )
    assertEquals(
      ranking.ratings.map(_.wins),
      Vector(0.5, 0.5),
    )
    assertEquals(
      ranking.ratings.map(_.score).distinct.size,
      1,
    )

  test("every judgement made is counted, however it was split"):
    val tied = List(Outcome(0, 1, 0.25), Outcome(1, 0, 0.25))
    assertEquals(
      rank(2, List(tied, List(Outcome(0, 1, 0.5)))).judgements,
      2,
    )

  test("failed judgements are reported, and not counted"):
    val failure = RuntimeException("No verdict.")
    val ranking = fitted(
      contest(2),
      List(
        Left(failure),
        Right(List(Outcome(0, 1, 0.5))),
      ),
    )
    assertEquals(ranking.failures, List(failure))
    assertEquals(ranking.judgements, 1)

  test("an uncalibrated ranking is scored from the contest's curve"):
    val ranking = rank(2, beat(0, 1), curve = Curve(50, 0))
    assertEquals(
      ranking.ratings.map(_.score),
      Vector(50.0, 50.0),
    )

  test("an uncalibrated ranking scores each item by its standing"):
    // Of two items, the stronger stands at the third quartile.
    val ranking = rank(2, beat(0, 1))
    assertEqualsDouble(
      ranking.ratings(0).score,
      0.6744898,
      1e-6,
    )
    assertEqualsDouble(
      ranking.ratings(1).score,
      -0.6744898,
      1e-6,
    )

  test("every item is rated, compared or not"):
    val ranking = rank(3, List(List(Outcome(0, 1, 0.5))))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0", "1", "2"),
    )

  test("an item between two anchors is scored between their scores"):
    // The anchors scored 40 and 80 are players 2 and 3.
    val ranking = rank(
      2,
      beat(0, 2) ++ beat(3, 0),
      anchors = ladder(40, 80),
    )
    val score = ranking.ratings(0).score
    assert(score > 40 && score < 80, score)

  test("items that all beat the anchors are all scored above them"):
    val swept = beat(3, 2) ++ (0 until 2)
      .toList
      .flatMap(item => beat(item, 2) ++ beat(item, 3))
    val ranking = rank(2, swept, anchors = ladder(40, 60))
    assert(
      ranking.ratings.forall(_.score > 60),
      ranking.ratings,
    )

  test("anchors ranked as they are scored are reported in full agreement"):
    val ranking = rank(1, beat(2, 1), anchors = ladder(40, 80))
    assertEquals(ranking.agreement, Some(1.0))

  test("anchors ranked against their scores are reported in disagreement"):
    // The anchor scored 40 (player 1) beats the one scored 80 (player 2).
    val ranking = rank(1, beat(1, 2), anchors = ladder(40, 80))
    assertEquals(ranking.agreement, Some(0.0))

  test("anchors stand on the ladder in score order, however they are given"):
    // Given highest first, the anchor scored 40 is still player 1.
    val ranking = rank(1, beat(2, 1), anchors = ladder(80, 40))
    assertEquals(ranking.agreement, Some(1.0))

  test("a ranking with no anchors reports no agreement"):
    assertEquals(rank(2, beat(0, 1)).agreement, None)

  test("only the items are rated, never the anchors they were placed against"):
    val ranking = rank(1, beat(0, 1), anchors = ladder(40, 80))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0"),
    )

object RankingSuite:

  private def beat(winner: Int, loser: Int): List[List[Outcome]] =
    List.fill(2)(List(Outcome(winner, loser, 0.5)))

  private def ladder(scores: Double*): List[Anchor[String]] = scores
    .toList
    .map(score => Anchor(s"Scored $score", score))

  private def contest
    (
      items: Int,
      anchors: List[Anchor[String]] = Nil,
      curve: Curve = Curve.standard,
    )
    : Contest[String] = Contest(
    List.tabulate(items)(_.toString),
    anchors,
    curve = curve,
  )

  private def fitted
    (
      contest: Contest[String],
      judgements: List[Judgement],
    )
    : Ranking[String] = Ranking.of(Field(contest), judgements)

  private def rank
    (
      items: Int,
      judgements: List[List[Outcome]],
      anchors: List[Anchor[String]] = Nil,
      curve: Curve = Curve.standard,
    )
    : Ranking[String] = fitted(
    contest(items, anchors, curve),
    judgements.map(Right(_)),
  )
