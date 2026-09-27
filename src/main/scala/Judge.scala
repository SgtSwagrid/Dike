package com.alecdorrington.dike

/**
  * Whatever decides which of two items is the better: a language model asked
  * the question, a person, or a match played between them. The items are given
  * in the order they are presented in. A judge may favour whichever it sees
  * first, which a [[Contest]] cancels out by asking both ways round, so the
  * judge needs no defence of its own against it.
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
