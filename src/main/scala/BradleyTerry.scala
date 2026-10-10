package com.alecdorrington.dike

/**
  * The Bradley-Terry model, which gives every player an ability such that a
  * player of log-ability `a` beats one of `b` with probability
  * `1 / (1 + exp(b - a))`. It is the Elo model, fitted from every comparison at
  * once.
  *
  * Abilities are fitted by maximum likelihood with Hunter's (2004)
  * minorisation-maximisation algorithm, which converges from any start. A weak
  * prior of imagined draws against an average opponent keeps the fit finite for
  * players who won or lost everything, and keeps players in disconnected parts
  * of the comparison graph comparable.
  */
object BradleyTerry:

  /**
    * One comparison, won by one player against another.
    *
    * @param winner
    *   The index of the player that won.
    *
    * @param loser
    *   The index of the player that lost.
    *
    * @param weight
    *   The share of a comparison this outcome counts for, as when a draw is
    *   split between the players.
    */
  final case class Outcome
    (
      winner: Int,
      loser: Int,
      weight: Double = 1.0,
    )

  /**
    * Fits an ability to every player from the comparisons between them.
    *
    * @param players
    *   The number of players, indexed from `0`.
    *
    * @param outcomes
    *   The comparisons to fit. Those naming a missing player, or one player
    *   twice, are ignored. A draw is two half-weighted outcomes, one each way.
    *
    * @return
    *   A log-ability per player, in player order. `0` is the prior's average
    *   opponent, and the ability of a player with no comparisons. Abilities are
    *   not centred on the players, so fits against the same reference players
    *   share a scale.
    */
  def abilities(players: Int, outcomes: Seq[Outcome]): Vector[Double] =
    val judged = outcomes.filter(valid(_, players))
    solve(
      wins(players, judged),
      meetings(players, judged),
    ).map(math.log)

  /**
    * Converts a log-ability to the Elo scale, on which the prior's average
    * opponent is `1500` and a lead of `400` points means winning nine
    * comparisons in ten.
    *
    * @param ability
    *   The log-ability, as given by [[abilities]].
    *
    * @return
    *   A rating on the Elo scale.
    */
  def elo(ability: Double): Double = average + ability * spread / math.log(10)

  private def valid(outcome: Outcome, players: Int): Boolean =
    Seq(outcome.winner, outcome.loser).forall(player =>
      player >= 0 && player < players,
    ) && outcome.winner != outcome.loser

  private[dike] def wins(players: Int, outcomes: Seq[Outcome]): Vector[Double] =
    val won = outcomes.groupMapReduce(_.winner)(_.weight)(_ + _)
    Vector.tabulate(players)(won.getOrElse(_, 0.0))

  /** Each player's opponents in order, with their total weight: only those met. */
  private type Meetings = Vector[Vector[(Int, Double)]]

  private def meetings(players: Int, outcomes: Seq[Outcome]): Meetings =
    val opponents = outcomes
      .groupMapReduce(outcome => pair(outcome.winner, outcome.loser))(_.weight)(
        _ + _,
      )
      .toVector
      .flatMap:
        case ((a, b), weight) => Vector(a -> (b -> weight), b -> (a -> weight))
      .groupMap(_._1)(_._2)
    Vector.tabulate(players)(player =>
      opponents.getOrElse(player, Vector.empty).sortBy(_._1),
    )

  private def pair(a: Int, b: Int): (Int, Int) = (a.min(b), a.max(b))

  private def solve(wins: Vector[Double], meetings: Meetings): Vector[Double] =
    Iterator
      .iterate(Vector.fill(wins.size)(1.0))(step(wins, meetings))
      .drop(steps)
      .next()

  /** One minorisation-maximisation update of every player's strength. */
  private def step
    (wins: Vector[Double], meetings: Meetings)
    (strengths: Vector[Double])
    : Vector[Double] = Vector.tabulate(strengths.size)(player =>
    (wins(player) + prior) / expected(player, strengths, meetings),
  )

  /**
    * The wins a player is expected to take at the current strengths, including
    * against the prior's average opponent, of strength `1`.
    */
  private def expected
    (
      player: Int,
      strengths: Vector[Double],
      meetings: Meetings,
    )
    : Double = meetings(player)
    .map((other, weight) => weight / (strengths(player) + strengths(other)))
    .sum + 2 * prior / (strengths(player) + 1)

  /** The draws every player is imagined to have had against the prior. */
  private val prior = 0.5

  private val steps = 1000

  private val average = 1500.0

  private val spread = 400.0
