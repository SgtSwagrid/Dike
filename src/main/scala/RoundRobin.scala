package com.alecdorrington.dike

/**
  * Scheduling of round-robin tournaments by the circle method: the players sit
  * in a circle and are paired across it, and every round all but one of them
  * shuffle round one seat. Each round therefore gives every player a different
  * opponent, and `players - 1` rounds exhaust every possible pairing exactly
  * once. Spreading comparisons evenly like this tells us far more about a field
  * than the same number of pairings drawn at random, which would leave some
  * players over-compared and others barely compared at all.
  */
object RoundRobin:

  /**
    * The pairings of the first few rounds of a round-robin tournament.
    *
    * @param players
    *   The number of players, indexed from `0`. When this is odd, a different
    *   player sits out each round.
    *
    * @param rounds
    *   The number of rounds to schedule, capped at the number needed to exhaust
    *   every pairing.
    *
    * @return
    *   The pairings to play, in round order, no pairing appearing twice.
    */
  def pairings(players: Int, rounds: Int): Seq[(Int, Int)] =
    val circle = seats(players)
    (0 until rounds.min(circle.size - 1).max(0)).flatMap(round(circle, _))

  /** The seats of the circle, with an empty one added if the count is odd. */
  private def seats(players: Int): Vector[Int] =
    val seated = (0 until players).toVector
    if players % 2 == 0 then seated else seated :+ empty

  /** The pairings of one round, every seat but the first having shuffled on. */
  private def round(seats: Vector[Int], turn: Int): Seq[(Int, Int)] =
    val shuffled = seats.head +: rotate(seats.tail, turn)
    shuffled
      .zip(shuffled.reverse)
      .take(seats.size / 2)
      .filterNot((a, b) => a == empty || b == empty)

  /** The given seats, moved along by the given number of places. */
  private def rotate(seats: Vector[Int], places: Int): Vector[Int] =
    val (front, back) = seats.splitAt(places % seats.size.max(1))
    back ++ front

  /** The seat that an odd player out is paired with, meaning no game. */
  private val empty = -1
