package com.alecdorrington.dike

import com.alecdorrington.dike.Calibration.Knot
import munit.FunSuite

/** Tests of the calibration of an ability scale against known scores. */
final class CalibrationSuite extends FunSuite:

  /** Two reference points spanning the middle of a scale. */
  private val ladder = Seq(Knot(-1, 40), Knot(1, 80))

  test("An ability between two reference points is read between them."):
    assertEquals(
      Calibration.score(ladder, 0),
      Some(60.0),
    )

  test("An ability at a reference point is read as its score."):
    assertEquals(
      Calibration.score(ladder, -1),
      Some(40.0),
    )

  test("An ability beyond the reference points is read past them."):
    assertEquals(
      Calibration.score(ladder, 2),
      Some(100.0),
    )
    assertEquals(
      Calibration.score(ladder, -1.5),
      Some(30.0),
    )

  test("A score read far beyond the reference points is left unbounded."):
    assertEquals(
      Calibration.score(ladder, 5),
      Some(160.0),
    )
    assertEquals(
      Calibration.score(ladder, -5),
      Some(-40.0),
    )

  test("The reference points may be given in any order."):
    assertEquals(
      Calibration.score(ladder.reverse, 0),
      Calibration.score(ladder, 0),
    )

  test("An ability is read between the nearest two of several points."):
    val rungs = Seq(Knot(0, 0), Knot(1, 50), Knot(2, 90))
    assertEquals(
      Calibration.score(rungs, 1.5),
      Some(70.0),
    )

  test("A single reference point does not determine a scale."):
    assertEquals(
      Calibration.score(Seq(Knot(0, 50)), 1),
      None,
    )

  test("No reference points determine no scale."):
    assertEquals(Calibration.score(Seq.empty, 1), None)

  test("Reference points of equal ability are merged, not divided by."):
    val tied = Seq(Knot(0, 40), Knot(0, 60), Knot(2, 70))
    assertEquals(Calibration.score(tied, 1), Some(60.0))

  test("Reference points of equal ability alone determine no scale."):
    assertEquals(
      Calibration.score(Seq(Knot(0, 40), Knot(0, 60)), 1),
      None,
    )

  test("Reference points that rank as they are scored agree fully."):
    assertEquals(
      Calibration.agreement(ladder),
      Some(1.0),
    )

  test("Reference points that rank against their scores disagree fully."):
    val inverted = Seq(Knot(-1, 80), Knot(1, 40))
    assertEquals(
      Calibration.agreement(inverted),
      Some(0.0),
    )

  test("Agreement is the share of the pairs that rank as they are scored."):
    // The middle point is scored above the highest, so of the three pairs, the
    // one that puts those two against each other is the only one to disagree.
    val muddled = Seq(Knot(0, 10), Knot(1, 90), Knot(2, 50))
    assertEquals(
      Calibration.agreement(muddled),
      Some(2.0 / 3),
    )

  test("Equal reference points each count in every pair they make."):
    // Of the six pairs, the one of the two equal points is level in both, so
    // neither agrees nor disagrees. Each of those two disagrees with the point
    // after it, scored lower, and agrees with the last, so three of the other
    // five pairs agree.
    val repeated = Seq(
      Knot(0, 50),
      Knot(0, 50),
      Knot(1, 40),
      Knot(2, 60),
    )
    assertEquals(
      Calibration.agreement(repeated),
      Some(3.0 / 5),
    )

  test("Reference points with nothing to disagree about report no agreement."):
    assertEquals(
      Calibration.agreement(Seq(Knot(0, 50), Knot(1, 50))),
      None,
    )
    assertEquals(
      Calibration.agreement(Seq(Knot(0, 50))),
      None,
    )
