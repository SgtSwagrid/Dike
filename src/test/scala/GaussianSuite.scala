package com.alecdorrington.dike

import munit.FunSuite

class GaussianSuite extends FunSuite:

  test("the median maps to zero"):
    assertEqualsDouble(Gaussian.quantile(0.5), 0.0, 1e-9)

  test("central quantiles are reproduced"):
    assertEqualsDouble(
      Gaussian.quantile(0.975),
      1.959964,
      1e-5,
    )
    assertEqualsDouble(Gaussian.quantile(0.75), 0.674490, 1e-5)

  test("tail quantiles are reproduced"):
    assertEqualsDouble(
      Gaussian.quantile(0.01),
      -2.326348,
      1e-5,
    )
    assertEqualsDouble(
      Gaussian.quantile(0.999),
      3.090232,
      1e-5,
    )

  test("the function is antisymmetric about the median"):
    List(0.01, 0.1, 0.25, 0.4).foreach: p =>
      assertEqualsDouble(
        Gaussian.quantile(p),
        -Gaussian.quantile(1 - p),
        1e-9,
      )

  test("probabilities outside the open unit interval are rejected"):
    intercept[IllegalArgumentException](Gaussian.quantile(0))
    intercept[IllegalArgumentException](Gaussian.quantile(1))
