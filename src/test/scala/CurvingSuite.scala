package com.alecdorrington.dike

import munit.FunSuite

final class CurvingSuite extends FunSuite:

  private val curve = Curve(1500, 200)

  test("a value stands at the share of the sample below it"):
    assertEqualsDouble(
      Curving.percentile(Seq(1, 2, 3, 4), 4),
      0.875,
      1e-9,
    )

  test("equal values share the ranks they span"):
    assertEqualsDouble(
      Curving.percentile(Seq(1, 2, 2, 3), 2),
      0.5,
      1e-9,
    )

  test("the sole value of a sample stands in the middle"):
    assertEqualsDouble(Curving.percentile(Seq(7), 7), 0.5, 1e-9)

  test("the middle of a curve is its mean"):
    assertEqualsDouble(Curving.score(0.5, curve), 1500.0, 1e-9)

  test("the third quartile lies two-thirds of a deviation above the mean"):
    assertEqualsDouble(
      Curving.score(0.75, curve),
      1500 + 200 * 0.6744898,
      1e-4,
    )

  test("a score read off a curve is left unbounded"):
    assert(Curving.score(0.999, curve) > curve.mean + 3 * curve.deviation)
    assert(Curving.score(0.001, curve) < curve.mean - 3 * curve.deviation)

  test("a value is normalised to the score at its standing in the sample"):
    assertEqualsDouble(
      Curving.normalised(Seq(1, 2, 3, 4), 4, curve),
      Curving.score(0.875, curve),
      1e-9,
    )

  test("the sole value of a sample is normalised to the curve's mean"):
    assertEqualsDouble(
      Curving.normalised(Seq(7), 7, curve),
      1500.0,
      1e-9,
    )

  test("normalisation keeps nothing of the sample but its order"):
    assertEqualsDouble(
      Curving.normalised(Seq(1, 2, 3, 4), 3, curve),
      Curving.normalised(Seq(-50, 0, 0.5, 900), 0.5, curve),
      1e-9,
    )
