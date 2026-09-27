package com.alecdorrington.dike

/**
  * Normalisation of scores onto a [[Curve]] by rank. A value is ranked among a
  * sample of comparable values, and the score at the same percentile of the
  * curve is given in its place. Only the order of the sample survives, so
  * values given by different judges become comparable however harsh or generous
  * each of them is.
  */
object Curving:

  /**
    * The mid-rank (Hazen) percentile of a value within a sample that contains
    * it. Ties share the ranks they span, so the percentile lies strictly
    * between `0` and `1`, and the best and worst of a sample are never given
    * the unreachable extremes of a curve.
    */
  def percentile(sample: Seq[Double], value: Double): Double =
    (sample.count(_ < value) + sample.count(_ == value) / 2.0) / sample.size

  /**
    * The score found at the given percentile of a curve.
    *
    * @param percentile
    *   The percentile, strictly between `0` and `1`.
    *
    * @param curve
    *   The curve to read the score off.
    *
    * @return
    *   A score, unbounded, as a curve has no ends. Clamp it to the range of the
    *   scale being read, if it has one.
    */
  def score(percentile: Double, curve: Curve): Double = curve.mean +
    Gaussian.quantile(percentile) * curve.deviation
