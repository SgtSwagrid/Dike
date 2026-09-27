package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import munit.FunSuite

/** Tests of the verdicts reached on one pairing judged both ways round. */
final class DuelSuite extends FunSuite:

  test("A verdict means the opposite when the items are swapped round."):
    assertEquals(Verdict.First.reversed, Verdict.Second)
    assertEquals(Verdict.Second.reversed, Verdict.First)
    assertEquals(Verdict.Tie.reversed, Verdict.Tie)

  test("A won judgement counts half a comparison to its winner."):
    assertEquals(
      Duel.outcomes(Verdict.Second, (3, 5)),
      List(Outcome(5, 3, 0.5)),
    )

  test("A drawn judgement counts a quarter of a comparison to each."):
    assertEquals(
      Duel.outcomes(Verdict.Tie, (3, 5)),
      List(Outcome(3, 5, 0.25), Outcome(5, 3, 0.25)),
    )

  test("Agreeing judgements reach their verdict."):
    val duel = Duel.of(
      List(
        Right(Verdict.First),
        Right(Verdict.First),
      ),
      (0, 1),
    )
    assertEquals(duel.verdict, Some(Verdict.First))

  test("Contradictory judgements reach no verdict."):
    val duel = Duel.of(
      List(
        Right(Verdict.First),
        Right(Verdict.Second),
      ),
      (0, 1),
    )
    assertEquals(duel.verdict, None)

  test("The one judgement that did not fail reaches the verdict."):
    val failure = RuntimeException("No verdict.")
    val duel    = Duel.of(
      List(Left(failure), Right(Verdict.Second)),
      (0, 1),
    )
    assertEquals(duel.verdict, Some(Verdict.Second))
    assertEquals(
      duel.judgements,
      List(
        Left(failure),
        Right(List(Outcome(1, 0, 0.5))),
      ),
    )

  test("A pairing whose every judgement failed reaches no verdict."):
    val failure = RuntimeException("No verdict.")
    assertEquals(
      Duel
        .of(
          List(Left(failure), Left(failure)),
          (0, 1),
        )
        .verdict,
      None,
    )
