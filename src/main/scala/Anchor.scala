package com.alecdorrington.dike

/**
  * An item of known score, against which the items of a [[Contest]] are placed.
  * Comparisons fix only an order; anchors say what each place in it is worth.
  *
  * @tparam A
  *   The type of the item.
  *
  * @param item
  *   The item.
  *
  * @param score
  *   The score the item deserves, on the scale the ranking's scores are read
  *   on.
  */
final case class Anchor[+A](item: A, score: Double)
