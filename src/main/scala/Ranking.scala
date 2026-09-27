package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome

/**
  * The outcome of a [[Contest]].
  *
  * @param ratings
  *   One rating per item, in the order the items were given.
  *
  * @param judgements
  *   The number of verdicts the ranking was fitted from, which falls short of
  *   those asked for when some of them failed. Every pairing is judged twice,
  *   once each way round, so two judgements make one comparison.
  *
  * @param agreement
  *   How far the anchors' own scores agreed with the order the comparisons put
  *   them in, from `0` when every pair of them came out the wrong way round to
  *   `1` when all of them agreed, or `None` when there were too few anchors to
  *   tell (see [[Calibration.agreement]]). A low value means either that the
  *   anchors are scored inconsistently with one another, or that the judge is
  *   not judging what their scores measure, and the scores should not be
  *   trusted.
  *
  * @param failures
  *   The error of every judgement that failed, and so was left out.
  */
final case class Ranking[+A]
  (
    ratings: Vector[Rating[A]],
    judgements: Int,
    agreement: Option[Double],
    failures: List[Throwable],
  ):

  /** The ratings, strongest first. */
  def ranked: Vector[Rating[A]] = ratings.sortBy(-_.elo)

/**
  * One item's place in a [[Ranking]].
  *
  * @param item
  *   The item rated.
  *
  * @param score
  *   The item's score: read off the anchors it fell among, or, when there were
  *   too few of them to calibrate the ranking, its standing among the items
  *   read off the contest's curve. Scores are not bounded (see
  *   [[Calibration.score]] and [[Curving.score]]).
  *
  * @param elo
  *   The fitted ability on the Elo scale, on which a lead of `400` points means
  *   winning nine comparisons in ten. Unlike [[score]], this does not depend on
  *   the curve, and so measures only how the items ranked. Contests placed
  *   against the same anchors share a scale, and may be compared on it.
  *
  * @param wins
  *   The number of comparisons this item won. This is measured in comparisons,
  *   not judgements: every pairing is judged twice, so a won judgement counts
  *   `0.5` and a drawn one `0.25`.
  */
final case class Rating[+A]
  (
    item: A,
    score: Double,
    elo: Double,
    wins: Double,
  )

object Ranking:

  /** The ranking of a field, fitted to the judgements made between them. */
  private[dike] def fitted[A]
    (field: Field[A], judged: List[Judged])
    : Ranking[A] =
    val (failures, succeeded) = judged.partitionMap(identity)
    val fit                   = Fit(field, succeeded.flatten)
    Ranking(
      Vector.tabulate(field.size)(fit.rating),
      succeeded.size,
      Calibration.agreement(fit.knots),
      failures,
    )

  /**
    * The abilities fitted to the outcomes of a contest, and what they say of
    * each item.
    */
  private final class Fit[A](field: Field[A], outcomes: List[Outcome]):

    /** The fitted ability of every player. */
    private val abilities = BradleyTerry.fit(field.players.size, outcomes)

    /** The anchors as reference points, at their fitted abilities. */
    val knots: List[Calibration.Knot] = field.knots(abilities)

    /** One item's rating: its score, its fitted ability and its record. */
    def rating(item: Int): Rating[A] = Rating(
      field.players(item),
      score(item),
      BradleyTerry.elo(abilities(item)),
      outcomes.filter(_.winner == item).map(_.weight).sum,
    )

    /**
      * One item's score: read off the anchors it fell among, or, when there are
      * too few of them to say what any ability is worth, taken from its
      * standing among the items on the contest's curve.
      */
    private def score(item: Int): Double = Calibration
      .score(knots, abilities(item))
      .getOrElse(Curving.score(standing(item), field.contest.curve))

    /** One item's percentile among the items, the anchors left out. */
    private def standing(item: Int): Double = Curving.percentile(
      abilities.take(field.size),
      abilities(item),
    )
