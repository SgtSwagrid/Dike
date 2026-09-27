package com.alecdorrington.dike

/**
  * An item whose score is already known, against which the items of a
  * [[Contest]] are placed. Comparisons fix only the order of the items
  * compared, never what any place in that order is worth. Anchors say what it
  * is worth.
  *
  * @param item
  *   The item.
  *
  * @param score
  *   The score the item is known to deserve, on the scale the ranking's scores
  *   are to be read on.
  */
final case class Anchor[+A](item: A, score: Double)
