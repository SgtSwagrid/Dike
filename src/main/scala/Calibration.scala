package com.alecdorrington.dike

/**
  * Calibration of a fitted ability scale against reference points of known
  * score. An ability is scored by reading off the line between the reference
  * points it falls among, so scores follow from what the references are worth,
  * not from an assumed distribution.
  */
object Calibration:

  /**
    * An ability known to deserve a score.
    *
    * @param ability
    *   The ability.
    *
    * @param score
    *   The score it deserves.
    */
  final case class Knot(ability: Double, score: Double)

  /**
    * Scores an ability on the line between the two reference points it falls
    * between, or on the line through the nearest two when it falls beyond them.
    *
    * @param knots
    *   The reference points, in any order. Those of equal ability count as one
    *   of their average score.
    *
    * @param ability
    *   The ability to score.
    *
    * @return
    *   An unbounded score, or `None` when fewer than two reference points of
    *   distinct ability were given.
    */
  def score(knots: Seq[Knot], ability: Double): Option[Double] =
    val points = ordered(knots)
    Option.when(points.sizeIs >= 2)(read(bracket(points, ability), ability))

  /**
    * Measures how far reference points agree with one another: the share of
    * their pairs ranked the same way by ability as by score. A pair level in
    * ability or in score neither agrees nor disagrees.
    *
    * @param knots
    *   The reference points, in any order.
    *
    * @return
    *   An agreement from `0`, when every pair disagrees, to `1`, when none
    *   does, or `None` when no pair differs in both ability and score.
    */
  def agreement(knots: Seq[Knot]): Option[Double] =
    val pairs = knots
      .tails
      .toList
      .flatMap:
        case first +: rest => rest.map((first, _))
        case _             => Nil
      .filter((a, b) => a.ability != b.ability && a.score != b.score)
    Option.when(pairs.nonEmpty)(pairs.count(concordant).toDouble / pairs.size)

  private def concordant(pair: (Knot, Knot)): Boolean =
    val (a, b) = pair
    (a.ability < b.ability) == (a.score < b.score)

  /** The reference points in ability order, those of equal ability merged. */
  private def ordered(knots: Seq[Knot]): Seq[Knot] = knots
    .groupBy(_.ability)
    .map((ability, same) =>
      Knot(
        ability,
        same.map(_.score).sum / same.size,
      ),
    )
    .toSeq
    .sortBy(_.ability)

  /**
    * The two reference points either side of the ability, or the nearest pair
    * when it lies beyond them all.
    */
  private def bracket(knots: Seq[Knot], ability: Double): (Knot, Knot) =
    val above = knots.indexWhere(_.ability > ability)
    val upper = if above < 0 then knots.size - 1 else above.max(1)
    (knots(upper - 1), knots(upper))

  private def read(bracket: (Knot, Knot), ability: Double): Double =
    val (low, high) = bracket
    low.score + (high.score - low.score) * (ability - low.ability) /
      (high.ability - low.ability)
