package com.alecdorrington.dike

import cats.{MonadThrow, Parallel}
import cats.syntax.functor.*

/**
  * A ranking to be made of some items by judging them in pairs.
  *
  * Every pairing is judged twice, once with each item presented first, to
  * cancel any preference of the judge for the first: agreeing judgements make a
  * win, contradictory ones a draw. The items meet one another on a
  * [[RoundRobin]] schedule, and each is placed on the ladder of anchors by
  * binary search, with each anchor also compared with the next one up. All of
  * it is fitted together by [[BradleyTerry]].
  *
  * Two or more anchors calibrate the fitted scale (see [[Calibration]]). With
  * fewer, each item is scored by its standing among the others, read off the
  * [[curve]] (see [[Curving]]).
  *
  * @tparam A
  *   The type of the items.
  *
  * @param items
  *   The items to rank.
  *
  * @param anchors
  *   The items of known score, in any order, against which the items are
  *   placed. They are not themselves ranked.
  *
  * @param rounds
  *   The number of comparisons each item has with the others, each against a
  *   different opponent, capped where every pairing is exhausted. `0` places
  *   the items against the anchors alone.
  *
  * @param curve
  *   The distribution the items' standings are mapped onto when there are too
  *   few anchors to calibrate the ranking.
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
    * @tparam F
    *   The effect the judge works in.
    *
    * @param judge
    *   The judge of every comparison. Judgements run in parallel wherever none
    *   waits on another's verdict, so a judge that costs something per call
    *   should limit its own concurrency.
    *
    * @return
    *   An effect producing the ranking. It does not fail with the judge: a
    *   failed judgement is left out and reported in [[Ranking.failures]].
    */
  def rank[F[_] : {MonadThrow, Parallel}](judge: Judge[F, A]): F[Ranking[A]] =
    val field = Field(this)
    Judging(field, judge).judgements.map(Ranking.of(field, _))

  /**
    * The most judgements the contest may call for, for pricing a judge before
    * it runs. Fewer are usually made, as a climb up the ladder ends early on a
    * draw.
    */
  def maxJudgements: Int = Field(this).maxJudgements

  /**
    * Counts the judgements sure to present each item and anchor first, whatever
    * the verdicts: one for either side of every pairing on the schedule, and of
    * each item's first rung up the ladder. A judge that keeps what it reads of
    * the item presented first can tell from this which it will read again.
    *
    * @return
    *   Every item and anchor that such a judgement presents first, with how
    *   many do.
    */
  def presentedFirst: Map[A, Int] = Field(this).presentedFirst
