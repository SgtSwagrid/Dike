package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome

/**
  * The outcome of a [[Contest]].
  *
  * @tparam A
  *   The type of the items.
  *
  * @param ratings
  *   The rating of each item, in the order the items were given.
  *
  * @param judgements
  *   The number of judgements the ranking was fitted from, fewer than asked for
  *   when some failed. Two judgements make one comparison.
  *
  * @param agreement
  *   The agreement, from `0` to `1`, of the anchors' scores with the order the
  *   comparisons put them in (see [[Calibration.agreement]]), or `None` with
  *   too few anchors to tell. A low value means the anchors are inconsistent or
  *   the judge measures something else, so the scores should not be trusted.
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
  * @tparam A
  *   The type of the item.
  *
  * @param item
  *   The item rated.
  *
  * @param score
  *   The unbounded score, read off the anchors the item fell among, or, with
  *   too few anchors, off the contest's curve by its standing among the items.
  *
  * @param elo
  *   The fitted ability on the Elo scale (see [[BradleyTerry.elo]]), which does
  *   not depend on the curve. Contests against the same anchors share it.
  *
  * @param wins
  *   The number of comparisons won, a judgement won counting `0.5` and a
  *   judgement drawn `0.25`.
  */
final case class Rating[+A]
  (
    item: A,
    score: Double,
    elo: Double,
    wins: Double,
  )

object Ranking:

  private[dike] def of[A]
    (
      field: Field[A],
      judgements: List[Judgement],
    )
    : Ranking[A] =
    val (failures, succeeded) = judgements.partitionMap(identity)
    val fit                   = Fit(field, succeeded.flatten)
    Ranking(
      Vector.tabulate(field.items)(fit.rating),
      succeeded.size,
      Calibration.agreement(fit.knots),
      failures,
    )

  private final class Fit[A](field: Field[A], outcomes: List[Outcome]):

    private val abilities = BradleyTerry.abilities(field.players.size, outcomes)

    private val items = abilities.take(field.items)

    private val won = BradleyTerry.wins(field.players.size, outcomes)

    val knots: List[Calibration.Knot] = field.knots(abilities)

    def rating(item: Int): Rating[A] = Rating(
      field.players(item),
      score(item),
      BradleyTerry.elo(abilities(item)),
      won(item),
    )

    private def score(item: Int): Double = Calibration
      .score(knots, abilities(item))
      .getOrElse(Curving.normalised(
        items,
        abilities(item),
        field.contest.curve,
      ))
