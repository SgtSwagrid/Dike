package com.alecdorrington.dike

/**
  * Calibration of a fitted ability scale against reference points of known
  * score. Comparisons fix only the order of the items compared, never what any
  * place in that order is worth. But when some of the items compared were
  * anchors whose scores are already known, the rest can be scored by reading
  * off the line between the anchors they fell among, so that scores follow from
  * what the anchors are worth rather than from any assumption about how the
  * field ought to be distributed. A field of uniformly strong items is then
  * free to score uniformly well.
  */
object Calibration:

  /** An ability that is known to deserve a given score. */
  final case class Knot(ability: Double, score: Double)

  /**
    * The score an ability deserves according to the given reference points: the
    * score on the line between the two it falls between, or on the line through
    * the nearest two when it falls beyond them altogether.
    *
    * @param knots
    *   The reference points to read between, in any order. Those of equal
    *   ability are treated as one of their average score.
    *
    * @param ability
    *   The ability to score.
    *
    * @return
    *   A score, unbounded, as a line read past the reference points has no end.
    *   `None` when fewer than two reference points of distinct ability were
    *   given, which do not determine a scale.
    */
  def score(knots: Seq[Knot], ability: Double): Option[Double] =
    val points = ordered(knots)
    Option.when(points.sizeIs >= 2)(read(bracket(points, ability), ability))

  /**
    * How far a set of reference points agree with one another: the share of
    * their pairs ranked the same way round by ability as by score, from `0`
    * when every pair disagrees to `1` when none does. Every point is paired
    * with every other, even one equal to it, and a pair level in ability or in
    * score is ranked neither way round, so neither agrees nor disagrees.
    *
    * @return
    *   An agreement, or `None` when no pair of the points differs in both
    *   ability and score, leaving nothing to agree or disagree about.
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

  /** Whether two reference points rank the same way by ability as by score. */
  private def concordant(points: (Knot, Knot)): Boolean =
    val (a, b) = points
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
    * The two reference points to read a score between: those either side of the
    * ability, or the nearest pair when it lies beyond them all.
    */
  private def bracket(points: Seq[Knot], ability: Double): (Knot, Knot) =
    val above = points.indexWhere(_.ability > ability)
    val upper = if above < 0 then points.size - 1 else above.max(1)
    (points(upper - 1), points(upper))

  /** The score at an ability on the line through two reference points. */
  private def read(bracket: (Knot, Knot), ability: Double): Double =
    val (low, high) = bracket
    low.score + (high.score - low.score) * (ability - low.ability) /
      (high.ability - low.ability)
