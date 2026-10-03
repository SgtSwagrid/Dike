package com.alecdorrington.dike

import com.alecdorrington.dike.Calibration.Knot

/**
  * The players of a [[Contest]], indexed first by the items, in the order
  * given, then by the anchors up the ladder, lowest score first.
  */
private[dike] final case class Field[A](contest: Contest[A]):

  val ladder: Vector[Anchor[A]] = contest.anchors.sortBy(_.score).toVector

  val players: Vector[A] = contest.items.toVector ++ ladder.map(_.item)

  /** The number of items to rank, which are the first of the players. */
  val items: Int = contest.items.size

  /** The player on the given rung of the ladder. */
  def onRung(rung: Int): Int = items + rung

  /** The pairings to judge whatever any verdict turns out to be. */
  def schedule: List[(Int, Int)] = peerings ++ rungs

  def peerings: List[(Int, Int)] = RoundRobin
    .pairings(items, contest.rounds)
    .toList

  /**
    * Each anchor paired with the next one up. Without these, items that beat
    * every anchor would leave the anchors' abilities indistinguishable, and
    * calibration impossible.
    */
  def rungs: List[(Int, Int)] =
    val climbed = ladder.indices.map(onRung)
    climbed.zip(climbed.drop(1)).toList

  def maxJudgements: Int = Comparison.sides *
    (schedule.size + items * Field.longestClimb(ladder.size))

  /**
    * The pairings judged whatever the verdicts: the schedule, and each item
    * against its climb's first rung.
    */
  def certain: List[(Int, Int)] =
    if ladder.isEmpty then schedule
    else
      val start = onRung(Field.middle(ladder.indices))
      schedule ++ List.range(0, items).map(_ -> start)

  def presentedFirst: Map[A, Int] = certain
    .flatMap((first, second) => List(first, second))
    .groupMapReduce(players)(_ => 1)(_ + _)

  def knots(abilities: Vector[Double]): List[Knot] = ladder
    .indices
    .map(rung =>
      Knot(
        abilities(onRung(rung)),
        ladder(rung).score,
      ),
    )
    .toList

private[dike] object Field:

  /**
    * The most rungs a binary search compares with on a ladder of the given
    * height, each verdict leaving at most the longer half.
    */
  def longestClimb(rungs: Int): Int =
    if rungs <= 0 then 0 else 1 + longestClimb(rungs / 2)

  /** The rung a climb over the given range of the ladder is compared with next. */
  def middle(rungs: Range): Int = rungs(rungs.size / 2)
