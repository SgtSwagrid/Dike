package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome

/** The outcomes of one judgement, or the error that prevented it. */
private[dike] type Judged = Either[Throwable, List[Outcome]]

/**
  * One pairing judged both ways round, each player presented first once, so
  * that no preference for whichever the judge saw first survives into the
  * result.
  *
  * @param judgements
  *   The outcomes of each judgement, or the error that prevented it.
  *
  * @param verdict
  *   The verdict every judgement made reached, when they agreed on one.
  */
private[dike] final case class Duel
  (
    judgements: List[Judged],
    verdict: Option[Verdict],
  )

private[dike] object Duel:

  /**
    * How much of a comparison one judgement counts for. Every pairing is judged
    * twice, once each way round, so that the two together carry the weight of a
    * single comparison.
    */
  val judgement = 0.5

  /**
    * A pairing's judgements, gathered with the verdict they agreed on.
    *
    * @param verdicts
    *   The verdict of each judgement, stated for the pairing in the order
    *   given, or the error that prevented it.
    *
    * @param pair
    *   The players judged.
    */
  def of
    (
      verdicts: List[Either[Throwable, Verdict]],
      pair: (Int, Int),
    )
    : Duel = Duel(
    verdicts.map(_.map(outcomes(_, pair))),
    agreed(verdicts.flatMap(_.toOption)),
  )

  /**
    * The verdict every judgement of a pairing reached, when they all reached
    * the same one. Contradictory judgements agree on nothing, which is the
    * signal that two players are too alike to tell apart.
    */
  private def agreed(verdicts: List[Verdict]): Option[Verdict] =
    verdicts.distinct match
      case List(only) => Some(only)
      case _          => None

  /** A verdict as outcomes, a draw counting half a judgement for each player. */
  def outcomes(verdict: Verdict, pair: (Int, Int)): List[Outcome] =
    val (first, second) = pair
    verdict match
      case Verdict.First  => List(Outcome(first, second, judgement))
      case Verdict.Second => List(Outcome(second, first, judgement))
      case Verdict.Tie    => List(
          Outcome(first, second, judgement / 2),
          Outcome(second, first, judgement / 2),
        )
