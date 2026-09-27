package com.alecdorrington.dike

import cats.{MonadThrow, Parallel}
import cats.syntax.all.*

/**
  * The judging of a [[Contest]]: every judgement it calls for, made in parallel
  * wherever none waits on another's verdict.
  *
  * @param field
  *   The players of the contest.
  *
  * @param judge
  *   The judge of every comparison.
  */
private[dike] final class Tournament[F[_] : {MonadThrow, Parallel}, A]
  (field: Field[A], judge: Judge[F, A]):

  /** Every judgement made, those scheduled and those of every climb alike. */
  def judgements: F[List[Judged]] = (scheduled, climbs).parMapN(_ ++ _)

  /** The judgements of every pairing known in advance of any verdict. */
  private def scheduled: F[List[Judged]] = field
    .schedule
    .parFlatTraverse(duel(_).map(_.judgements))

  /** The judgements placing each item on the ladder. */
  private def climbs: F[List[Judged]] = List
    .range(0, field.size)
    .parFlatTraverse(climb(_, field.ladder.indices))

  /**
    * Places one item on the ladder by binary search: it is compared with the
    * middle rung of the range it might belong to, and then with the middle of
    * whichever half of that range the verdict leaves, until no rung is left. A
    * drawn or contradictory verdict ends the climb, the item having been placed
    * as precisely as the ladder allows.
    */
  private def climb(item: Int, rungs: Range): F[List[Judged]] =
    if rungs.isEmpty then List.empty[Judged].pure[F]
    else reach(item, rungs, rungs(rungs.size / 2))

  /** One rung of a climb: judge against it, then narrow the range and go on. */
  private def reach(item: Int, rungs: Range, rung: Int): F[List[Judged]] =
    duel((item, field.rung(rung))).flatMap(judged =>
      climb(
        item,
        Tournament.narrowed(rungs, rung, judged.verdict),
      ).map(judged.judgements ++ _),
    )

  /** Judges one pairing both ways round, in parallel. */
  private def duel(pair: (Int, Int)): F[Duel] = List(
    ask(pair),
    ask(pair.swap).map(_.reversed),
  ).parTraverse(_.attempt).map(Duel.of(_, pair))

  /**
    * Asks the judge about two players, the first of the pair presented first. A
    * judge that throws rather than failing in `F` fails the judgement all the
    * same.
    */
  private def ask(order: (Int, Int)): F[Verdict] =
    val (first, second) = order
    MonadThrow[F]
      .catchNonFatal(judge(
        field.players(first),
        field.players(second),
      ))
      .flatten

private[dike] object Tournament:

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
