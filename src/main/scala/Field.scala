package com.alecdorrington.dike

import com.alecdorrington.dike.Calibration.Knot

/**
  * The players of a [[Contest]], indexed as the comparisons between them are
  * recorded: first the items, in the order given, then the anchors up the
  * ladder, from the lowest score to the highest.
  *
  * @param contest
  *   The contest the players are taking part in.
  */
private[dike] final case class Field[A](contest: Contest[A]):

  /** The anchors in score order, lowest first, as the ladder is climbed. */
  val ladder: Vector[Anchor[A]] = contest.anchors.sortBy(_.score).toVector

  /** Every player, the items first and then the anchors up the ladder. */
  val players: Vector[A] = contest.items.toVector ++ ladder.map(_.item)

  /** The number of items to rank, which are the first of the players. */
  val size: Int = contest.items.size

  /** The player standing on the given rung of the ladder. */
  def rung(step: Int): Int = size + step

  /** The pairings to judge whatever any verdict turns out to be. */
  def schedule: List[(Int, Int)] = peerings ++ rungs

  /**
    * The pairings of the items with one another. Each round pairs every item
    * with a different opponent, so that the comparisons are spread evenly over
    * the field rather than leaving some items barely compared.
    */
  def peerings: List[(Int, Int)] = RoundRobin
    .pairings(size, contest.rounds)
    .toList

  /**
    * Each anchor paired with the one scored next above it. Judging the ladder
    * against itself settles the order of its own rungs, which the items'
    * results alone need not pin down: were every item to beat every anchor,
    * nothing would distinguish one anchor from another, and the scores they are
    * meant to calibrate could not be read between them.
    */
  def rungs: List[(Int, Int)] =
    val climbed = ladder.indices.map(rung)
    climbed.zip(climbed.drop(1)).toList

  /** The anchors as reference points, at the abilities fitted to them. */
  def knots(abilities: Vector[Double]): List[Knot] = ladder
    .indices
    .map(step =>
      Knot(
        abilities(rung(step)),
        ladder(step).score,
      ),
    )
    .toList
