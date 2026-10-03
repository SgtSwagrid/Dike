package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import munit.FunSuite

final class ComparisonSuite extends FunSuite:

  test("a verdict means the opposite when the items are swapped round"):
    assertEquals(Verdict.First.reversed, Verdict.Second)
    assertEquals(Verdict.Second.reversed, Verdict.First)
    assertEquals(Verdict.Tie.reversed, Verdict.Tie)

  test("a won judgement counts half a comparison to its winner"):
    assertEquals(
      Comparison.outcomes(Verdict.Second, (3, 5)),
      List(Outcome(5, 3, 0.5)),
    )

  test("a drawn judgement counts a quarter of a comparison to each"):
    assertEquals(
      Comparison.outcomes(Verdict.Tie, (3, 5)),
      List(Outcome(3, 5, 0.25), Outcome(5, 3, 0.25)),
    )

  test("agreeing judgements reach their verdict"):
    val comparison = Comparison.of(
      List(
        Right(Verdict.First),
        Right(Verdict.First),
      ),
      (0, 1),
    )
    assertEquals(
      comparison.verdict,
      Some(Verdict.First),
    )

  test("contradictory judgements reach no verdict"):
    val comparison = Comparison.of(
      List(
        Right(Verdict.First),
        Right(Verdict.Second),
      ),
      (0, 1),
    )
    assertEquals(comparison.verdict, None)

  test("the one judgement that did not fail reaches the verdict"):
    val failure    = RuntimeException("No verdict.")
    val comparison = Comparison.of(
      List(Left(failure), Right(Verdict.Second)),
      (0, 1),
    )
    assertEquals(
      comparison.verdict,
      Some(Verdict.Second),
    )
    assertEquals(
      comparison.judgements,
      List(
        Left(failure),
        Right(List(Outcome(1, 0, 0.5))),
      ),
    )

  test("a pairing whose every judgement failed reaches no verdict"):
    val failure = RuntimeException("No verdict.")
    assertEquals(
      Comparison
        .of(
          List(Left(failure), Left(failure)),
          (0, 1),
        )
        .verdict,
      None,
    )
