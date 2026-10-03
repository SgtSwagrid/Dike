package com.alecdorrington.dike

/** A [[Judge]]'s finding of which of two items is the better, if either. */
enum Verdict:

  /** The item presented first is the better. */
  case First

  /** The item presented second is the better. */
  case Second

  /** Neither item is the better. */
  case Tie

  /** This verdict restated for the two items presented the other way round. */
  def reversed: Verdict = this match
    case First  => Second
    case Second => First
    case Tie    => Tie
