package com.alecdorrington.dike

import cats.{MonadThrow, Parallel}
import cats.syntax.functor.*

/**
  * A ranking to be made of some items by judging them in pairs. A judge says
  * far more consistently which of two items is the better than what either is
  * worth alone, so a ranking fitted from many such judgements is the steadier
  * measure.
  *
  * Every pairing is judged twice, once with either item presented first, which
  * cancels out any preference the judge has for whichever it saw first:
  * agreeing judgements amount to a win, and contradictory ones to a draw. Two
  * kinds of pairing are judged, and both are fitted together by
  * [[BradleyTerry]] as one body of evidence:
  *
  *   - The items are paired with one another on a [[RoundRobin]] schedule,
  *     which settles their order among themselves.
  *   - Each item is placed on the ladder of anchors by binary search, which
  *     settles where that order sits in absolute terms, at a cost of only a
  *     handful of comparisons per item however long the ladder. Each anchor is
  *     also compared with the next one up, which settles the ladder's own
  *     order.
  *
  * The anchors' known scores then calibrate the fitted scale (see
  * [[Calibration]]). With fewer than two anchors, the fit gives only an order,
  * and each item is scored by its standing among the others, read off the
  * [[curve]] (see [[Curving]]).
  *
  * @param items
  *   The items to rank.
  *
  * @param anchors
  *   Items of known score, in any order, against which the items are placed.
  *   Two or more of them calibrate the ranking, so that scores follow from what
  *   the anchors are worth rather than from how the items are assumed to be
  *   distributed. They are not themselves ranked.
  *
  * @param rounds
  *   The number of comparisons each item takes part in against the others. Each
  *   round pairs every item with a different opponent, up to the number of
  *   rounds that exhausts every pairing. May be `0` to place the items against
  *   the anchors alone, without comparing them with one another.
  *
  * @param curve
  *   The distribution onto which the items' standings are mapped when there are
  *   too few anchors to calibrate the ranking.
  */
final case class Contest[A]
  (
    items: Seq[A],
    anchors: Seq[Anchor[A]] = Nil,
    rounds: Int = 1,
    curve: Curve = Curve.standard,
  ):

  /**
    * Makes every judgement the contest calls for, then ranks the items.
    *
    * @param judge
    *   The judge of every comparison. Judgements are asked for in parallel
    *   wherever none waits on another's verdict, so a judge that costs
    *   something per call should limit for itself how many run at once.
    *
    * @return
    *   A ranking of the items, which the judge cannot fail: a judgement that
    *   fails is left out of it, and reported in [[Ranking.failures]].
    */
  def rank[F[_] : {MonadThrow, Parallel}](judge: Judge[F, A]): F[Ranking[A]] =
    val field = Field(this)
    Tournament(field, judge).judgements.map(Ranking.fitted(field, _))
