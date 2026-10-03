package com.alecdorrington.dike

import cats.{MonadThrow, Parallel}
import cats.syntax.all.*

/**
  * The judging of a [[Contest]], making every judgement in parallel wherever
  * none waits on another's verdict.
  */
private[dike] final class Judging[F[_] : {MonadThrow, Parallel}, A]
  (field: Field[A], judge: Judge[F, A]):

  def judgements: F[List[Judgement]] = (scheduled, climbs).parMapN(_ ++ _)

  private def scheduled: F[List[Judgement]] = field
    .schedule
    .parFlatTraverse(compare(_).map(_.judgements))

  private def climbs: F[List[Judgement]] = List
    .range(0, field.items)
    .parFlatTraverse(climb(_, field.ladder.indices))

  /**
    * Places one item on the ladder by binary search. A drawn or contradictory
    * verdict ends the climb, as the item is then placed as precisely as the
    * ladder allows.
    */
  private def climb(item: Int, rungs: Range): F[List[Judgement]] =
    if rungs.isEmpty then List.empty[Judgement].pure[F]
    else reach(item, rungs, Field.middle(rungs))

  private def reach(item: Int, rungs: Range, rung: Int): F[List[Judgement]] =
    compare((item, field.onRung(rung))).flatMap(comparison =>
      climb(
        item,
        Judging.narrowed(rungs, rung, comparison.verdict),
      ).map(comparison.judgements ++ _),
    )

  private def compare(pair: (Int, Int)): F[Comparison] = List(
    ask(pair),
    ask(pair.swap).map(_.reversed),
  ).parTraverse(_.attempt).map(Comparison.of(_, pair))

  /** Asks the judge, catching a throw as a failure in `F`. */
  private def ask(order: (Int, Int)): F[Verdict] =
    val (first, second) = order
    MonadThrow[F]
      .catchNonFatal(judge(
        field.players(first),
        field.players(second),
      ))
      .flatten

private[dike] object Judging:

  /** The rungs left to search after a verdict against the middle one. */
  def narrowed
    (
      rungs: Range,
      rung: Int,
      verdict: Option[Verdict],
    )
    : Range = verdict match
    case Some(Verdict.First)  => rung + 1 until rungs.end
    case Some(Verdict.Second) => rungs.start until rung
    case _                    => Range(0, 0)
