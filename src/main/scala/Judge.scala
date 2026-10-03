package com.alecdorrington.dike

/**
  * A judge of which of two items is the better, such as a language model, a
  * person or a match, given the items in the order they are presented. It needs
  * no defence against favouring the first, as a [[Contest]] asks both ways
  * round.
  *
  * A judge may fail, in `F` or by throwing. Either way that one judgement is
  * left out of the ranking, and its error reported in [[Ranking.failures]].
  *
  * @tparam F
  *   The effect each judgement is made in.
  *
  * @tparam A
  *   The type of the items judged.
  */
type Judge[F[_], A] = (A, A) => F[Verdict]
