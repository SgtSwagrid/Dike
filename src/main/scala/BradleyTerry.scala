package com.alecdorrington.dike

/**
  * The Bradley-Terry model, which explains a collection of pairwise comparisons
  * by giving every player an ability, such that of two players the stronger
  * wins with probability `1 / (1 + exp(b - a))` in their log-abilities. It is
  * the same model as the Elo rating system, differing only in that abilities
  * are fitted from all comparisons at once rather than updated one at a time.
  *
  * Abilities are fitted by maximum likelihood, using the minorisation-
  * maximisation algorithm of Hunter (2004): each step is a closed-form update
  * that cannot decrease the likelihood, so the fit converges from any starting
  * point. A weak prior of imagined drawn comparisons against an average
  * opponent keeps the fit finite for players who won or lost everything, and
  * keeps players in disconnected parts of the comparison graph comparable.
  */
object BradleyTerry:

  /**
    * One comparison, won by one player against another.
    *
    * @param winner
    *   The index of the player that won the comparison.
    *
    * @param loser
    *   The index of the player that lost the comparison.
    *
    * @param weight
    *   How much of a comparison this outcome counts for, allowing a judgement
    *   to be split between the players or discounted for unreliability.
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
    *   The number of players, indexed from `0`. A player with no comparisons is
    *   fitted at the ability of the prior's imagined opponent, `0`.
    *
    * @param outcomes
    *   The comparisons to fit, those naming a player that does not exist being
    *   ignored. A draw is expressed as two half-weighted outcomes, one each
    *   way.
    *
    * @return
    *   A log-ability per player, in player order, on a scale where `0` is the
    *   prior's imagined opponent. Nothing is centred on the players, so fields
    *   fitted against the same anchors share a scale.
    */
  def fit(players: Int, outcomes: Seq[Outcome]): Vector[Double] =
    val judged = outcomes.filter(valid(_, players))
    solve(
      wins(players, judged),
      meetings(players, judged),
    ).map(math.log)

  /**
    * A log-ability expressed on the Elo scale, on which the prior's imagined
    * opponent sits at `1500`, and a lead of `400` points means winning nine
    * comparisons in ten.
    */
  def elo(ability: Double): Double = average + ability * spread / math.log(10)

  /** Whether a comparison is between two distinct players that exist. */
  private def valid(outcome: Outcome, players: Int): Boolean =
    Seq(outcome.winner, outcome.loser).forall(player =>
      player >= 0 && player < players,
    ) && outcome.winner != outcome.loser

  /** The total weight of the comparisons each player won. */
  private def wins(players: Int, outcomes: Seq[Outcome]): Vector[Double] =
    val won = outcomes.groupMapReduce(_.winner)(_.weight)(_ + _)
    Vector.tabulate(players)(won.getOrElse(_, 0.0))

  /** The total weight of the comparisons between each pair of players. */
  private def meetings
    (players: Int, outcomes: Seq[Outcome])
    : Vector[Vector[Double]] =
    val counted = outcomes.groupMapReduce(outcome =>
      pair(outcome.winner, outcome.loser),
    )(_.weight)(_ + _)
    Vector.tabulate(players, players)((a, b) =>
      counted.getOrElse(pair(a, b), 0.0),
    )

  /** Two players in ascending order, so that a pairing has one name. */
  private def pair(a: Int, b: Int): (Int, Int) = (a.min(b), a.max(b))

  /** Iterates the update to convergence, from an all-equal starting point. */
  private def solve
    (
      wins: Vector[Double],
      meetings: Vector[Vector[Double]],
    )
    : Vector[Double] = Iterator
    .iterate(Vector.fill(wins.size)(1.0))(step(wins, meetings))
    .drop(steps)
    .next()

  /** One minorisation-maximisation update of every player's strength. */
  private def step
    (
      wins: Vector[Double],
      meetings: Vector[Vector[Double]],
    )
    (strengths: Vector[Double])
    : Vector[Double] = Vector.tabulate(strengths.size)(player =>
    (wins(player) + prior) / expected(player, strengths, meetings),
  )

  /**
    * The wins a player would be expected to take from its comparisons at the
    * current strengths, including those against the prior's average opponent.
    */
  private def expected
    (
      player: Int,
      strengths: Vector[Double],
      meetings: Vector[Vector[Double]],
    )
    : Double = strengths
    .indices
    .map(other =>
      meetings(player)(other) / (strengths(player) + strengths(other)),
    )
    .sum + 2 * prior / (strengths(player) + 1)

  /**
    * The drawn comparisons every player is imagined to have had against an
    * opponent of average ability, which keep the fit finite and bounded.
    */
  private val prior = 0.5

  /**
    * The number of update steps taken. The algorithm converges geometrically
    * and each step costs only as much as the comparison graph is large, so this
    * is set generously rather than tested for convergence.
    */
  private val steps = 1000

  /** The Elo rating of a player of average ability. */
  private val average = 1500.0

  /** The Elo points between players of nine-in-ten and even odds. */
  private val spread = 400.0
