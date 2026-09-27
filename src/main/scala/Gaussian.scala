package com.alecdorrington.dike

/** Numerical routines for the standard normal distribution. */
object Gaussian:

  /**
    * The quantile function (inverse cumulative distribution function) of the
    * standard normal distribution, computed with Acklam's rational
    * approximation (relative error below `1.15e-9`).
    *
    * @param p
    *   A probability strictly between `0` and `1`.
    *
    * @return
    *   A z-score: the value below which a standard normal variable falls with
    *   probability `p`.
    */
  def quantile(p: Double): Double =
    require(
      p > 0 && p < 1,
      s"Probability out of range: $p.",
    )
    if p < low then tail(p) else if p > 1 - low then -tail(1 - p) else centre(p)

  /** The boundary between the central region and the tails. */
  private val low = 0.02425

  // Coefficients of Acklam's approximation, leading terms first.
  private val a = Vector(
    -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
    1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00,
  )

  private val b = Vector(
    -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
    6.680131188771972e+01, -1.328068155288572e+01,
  )

  private val c = Vector(
    -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
    -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00,
  )

  private val d = Vector(
    7.784695709041462e-03,
    3.224671290700398e-01,
    2.445134137142996e+00,
    3.754408661907416e+00,
  )

  /** The approximation on the central region `[low, 1 - low]`. */
  private def centre(p: Double): Double =
    val q = p - 0.5
    val r = q * q
    poly(a, r) * q / (poly(b, r) * r + 1)

  /** The approximation on the lower tail `(0, low)`. */
  private def tail(p: Double): Double =
    val q = math.sqrt(-2 * math.log(p))
    poly(c, q) / (poly(d, q) * q + 1)

  /** Evaluates a polynomial by Horner's method, leading coefficient first. */
  private def poly(coefficients: Vector[Double], x: Double): Double =
    coefficients.reduce(_ * x + _)
