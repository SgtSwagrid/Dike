package com.alecdorrington.dike

/** Numerical routines for the standard normal distribution. */
object Gaussian:

  /**
    * Computes the quantile function (inverse cumulative distribution function)
    * of the standard normal distribution by Acklam's rational approximation,
    * with relative error below `1.15e-9`.
    *
    * @param probability
    *   The probability, strictly between `0` and `1`, or the call throws.
    *
    * @return
    *   A z-score below which a standard normal variable falls with the given
    *   probability.
    */
  def quantile(probability: Double): Double =
    require(
      probability > 0 && probability < 1,
      s"Probability out of range: $probability.",
    )
    if probability < low then tail(probability)
    else if probability > 1 - low then -tail(1 - probability)
    else centre(probability)

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

  private def centre(p: Double): Double =
    val q = p - 0.5
    val r = q * q
    polynomial(a, r) * q / (polynomial(b, r) * r + 1)

  private def tail(p: Double): Double =
    val q = math.sqrt(-2 * math.log(p))
    polynomial(c, q) / (polynomial(d, q) * q + 1)

  private def polynomial(coefficients: Vector[Double], x: Double): Double =
    coefficients.reduce(_ * x + _)
