package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import munit.FunSuite

/** Tests of the rankings fitted to the judgements of a contest. */
final class RankingSuite extends FunSuite:

  import RankingSuite.*

  test("The stronger of two items is ranked and scored above the weaker."):
    val ranked = rank(2, beat(0, 1)).ranked
    assertEquals(ranked.map(_.item), Vector("0", "1"))
    assert(ranked.head.score > ranked.last.score)

  test("Ratings keep the order the items were given in."):
    val ranking = rank(2, beat(1, 0))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0", "1"),
    )
    assertEquals(
      ranking.ranked.map(_.item),
      Vector("1", "0"),
    )

  test("Agreeing judgements amount to one comparison won."):
    assertEqualsDouble(
      rank(2, beat(0, 1)).ratings(0).wins,
      1.0,
      1e-9,
    )

  test("Contradictory judgements amount to a draw."):
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

  test("Every judgement made is counted, however it was split."):
    val drawn = List(Outcome(0, 1, 0.25), Outcome(1, 0, 0.25))
    assertEquals(
      rank(2, List(drawn, List(Outcome(0, 1, 0.5)))).judgements,
      2,
    )

  test("Failed judgements are reported, and not counted."):
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

  test("An uncalibrated ranking is scored from the contest's curve."):
    val ranking = rank(2, beat(0, 1), curve = Curve(50, 0))
    assertEquals(
      ranking.ratings.map(_.score),
      Vector(50.0, 50.0),
    )

  test("An uncalibrated ranking scores each item by its standing."):
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

  test("Every item is rated, compared or not."):
    val ranking = rank(3, List(List(Outcome(0, 1, 0.5))))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0", "1", "2"),
    )

  test("An item between two anchors is scored between their scores."):
    // Items 0 and 1 are ranked, and the anchors scored 40 and 80 follow them.
    val ranking = rank(
      2,
      beat(0, 2) ++ beat(3, 0),
      anchors = anchored(40, 80),
    )
    val score = ranking.ratings(0).score
    assert(score > 40 && score < 80, score)

  test("Items that all beat the anchors are all scored above them."):
    val swept = beat(3, 2) ++ (0 until 2)
      .toList
      .flatMap(item => beat(item, 2) ++ beat(item, 3))
    val ranking = rank(2, swept, anchors = anchored(40, 60))
    assert(
      ranking.ratings.forall(_.score > 60),
      ranking.ratings,
    )

  test("Anchors ranked as they are scored are reported in full agreement."):
    val ranking = rank(1, beat(2, 1), anchors = anchored(40, 80))
    assertEquals(ranking.agreement, Some(1.0))

  test("Anchors ranked against their scores are reported in disagreement."):
    // The anchor scored 40 is player 1 and the one scored 80 is player 2, but
    // the lower-scored anchor beat the higher, so the two disagree.
    val ranking = rank(1, beat(1, 2), anchors = anchored(40, 80))
    assertEquals(ranking.agreement, Some(0.0))

  test("Anchors stand on the ladder in score order, however they are given."):
    // Given highest first, the anchor scored 40 is still player 1.
    val ranking = rank(1, beat(2, 1), anchors = anchored(80, 40))
    assertEquals(ranking.agreement, Some(1.0))

  test("A ranking with no anchors reports no agreement."):
    assertEquals(rank(2, beat(0, 1)).agreement, None)

  test("Only the items are rated, never the anchors they were placed against."):
    val ranking = rank(1, beat(0, 1), anchors = anchored(40, 80))
    assertEquals(
      ranking.ratings.map(_.item),
      Vector("0"),
    )

object RankingSuite:

  /** A pairing judged both ways round, the same winner both times. */
  private def beat(winner: Int, loser: Int): List[List[Outcome]] =
    List.fill(2)(List(Outcome(winner, loser, 0.5)))

  /** Anchors of the given scores, each named after its score. */
  private def anchored(scores: Double*): List[Anchor[String]] = scores
    .toList
    .map(score => Anchor(s"Scored $score", score))

  /** A contest of the given number of items, each named after its index. */
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

  /** Fits a ranking directly from prepared judgements. */
  private def fitted
    (
      contest: Contest[String],
      judged: List[Judged],
    )
    : Ranking[String] = Ranking.fitted(Field(contest), judged)

  /** Fits a ranking of the given number of items from prepared outcomes. */
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
