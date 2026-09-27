package com.alecdorrington.dike

import munit.FunSuite

/** Tests of the normalisation of scores onto a curve by rank. */
final class CurvingSuite extends FunSuite:

  test("A value stands at the share of the sample below it."):
    assertEqualsDouble(
      Curving.percentile(Seq(1, 2, 3, 4), 4),
      0.875,
      1e-9,
    )

  test("Equal values share the ranks they span."):
    assertEqualsDouble(
      Curving.percentile(Seq(1, 2, 2, 3), 2),
      0.5,
      1e-9,
    )

  test("The sole value of a sample stands in the middle."):
    assertEqualsDouble(Curving.percentile(Seq(7), 7), 0.5, 1e-9)

  test("The middle of a curve is its mean."):
    assertEqualsDouble(
      Curving.score(0.5, Curve(65, 15)),
      65.0,
      1e-9,
    )

  test("The third quartile lies two-thirds of a deviation above the mean."):
    assertEqualsDouble(
      Curving.score(0.75, Curve(65, 15)),
      65 + 15 * 0.6744898,
      1e-4,
    )

  test("A score read off a curve is left unbounded."):
    assert(Curving.score(0.999, Curve(65, 100)) > 100)
