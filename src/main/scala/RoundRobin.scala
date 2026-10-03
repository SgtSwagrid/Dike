package com.alecdorrington.dike

/**
  * Scheduling of round-robin tournaments by the circle method. Each round gives
  * every player a different opponent, and `players - 1` rounds exhaust every
  * pairing exactly once.
  */
object RoundRobin:

  /**
    * Schedules the pairings of the first rounds of a round-robin tournament.
    *
    * @param players
    *   The number of players, indexed from `0`. When it is odd, a different
    *   player sits out each round.
    *
    * @param rounds
    *   The number of rounds, capped at the number that exhausts every pairing.
    *
    * @return
    *   A sequence of pairings in round order, none appearing twice.
    */
  def pairings(players: Int, rounds: Int): Seq[(Int, Int)] =
    val circle = seats(players)
    (0 until rounds.min(circle.size - 1).max(0)).flatMap(round(circle, _))

  /** The seats of the circle, with a bye added if the count is odd. */
  private def seats(players: Int): Vector[Int] =
    val seated = (0 until players).toVector
    if players % 2 == 0 then seated else seated :+ bye

  /** The pairings of one round, every seat but the first having moved on. */
  private def round(seats: Vector[Int], turn: Int): Seq[(Int, Int)] =
    val shuffled = seats.head +: rotated(seats.tail, turn)
    shuffled
      .zip(shuffled.reverse)
      .take(seats.size / 2)
      .filterNot((a, b) => a == bye || b == bye)

  private def rotated(seats: Vector[Int], places: Int): Vector[Int] =
    val (front, back) = seats.splitAt(places % seats.size.max(1))
    back ++ front

  /** The seat an odd player out is paired with, meaning no game. */
  private val bye = -1
