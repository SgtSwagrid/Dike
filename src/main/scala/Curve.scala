package com.alecdorrington.dike

/**
  * A normal distribution of scores, onto which items are placed by their
  * standing among one another (see [[Curving]]).
  *
  * @param mean
  *   The score of an item of middling standing.
  *
  * @param deviation
  *   The standard deviation of the scores.
  */
final case class Curve(mean: Double, deviation: Double)

object Curve:

  /** The standard normal distribution, on which every score is a z-score. */
  val standard: Curve = Curve(0, 1)
