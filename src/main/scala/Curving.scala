package com.alecdorrington.dike

/**
  * Normalisation of scores onto a [[Curve]] by rank: a value is ranked among a
  * sample, and replaced by the score at the same percentile of the curve. Only
  * the sample's order survives, so values from harsh and generous judges become
  * comparable.
  */
object Curving:

  /**
    * Normalises a value onto a curve by its standing within a sample: ranks it
    * by [[percentile]], then reads the curve there by [[score]].
    *
    * @param sample
    *   The sample, which must contain the value.
    *
    * @param value
    *   The value to normalise.
    *
    * @param curve
    *   The curve to read the score off.
    *
    * @return
    *   An unbounded score, for the host to clamp to its scale.
    */
  def normalised
    (
      sample: Seq[Double],
      value: Double,
      curve: Curve,
    )
    : Double = score(percentile(sample, value), curve)

  /**
    * Computes the mid-rank (Hazen) percentile of a value within a sample. Ties
    * share the ranks they span.
    *
    * @param sample
    *   The sample, which must contain the value.
    *
    * @param value
    *   The value to rank.
    *
    * @return
    *   A percentile strictly between `0` and `1`, so never the unreachable
    *   extremes of a curve.
    */
  def percentile(sample: Seq[Double], value: Double): Double =
    (sample.count(_ < value) + sample.count(_ == value) / 2.0) / sample.size

  /**
    * Reads the score at a percentile of a curve.
    *
    * @param percentile
    *   The percentile, strictly between `0` and `1`.
    *
    * @param curve
    *   The curve to read the score off.
    *
    * @return
    *   An unbounded score, to be clamped to the range of its scale if it has
    *   one.
    */
  def score(percentile: Double, curve: Curve): Double = curve.mean +
    Gaussian.quantile(percentile) * curve.deviation
