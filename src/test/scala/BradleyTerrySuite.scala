package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import munit.FunSuite

/** Tests of the Bradley-Terry fit of abilities to pairwise comparisons. */
final class BradleyTerrySuite extends FunSuite:

  test("A player that beats another is fitted the stronger of the two."):
    val fitted = BradleyTerry.fit(2, Seq(Outcome(0, 1)))
    assert(fitted(0) > fitted(1))

  test("A transitive run of results is fitted in its own order."):
    val fitted = BradleyTerry.fit(
      3,
      Seq(
        Outcome(0, 1),
        Outcome(1, 2),
        Outcome(0, 2),
      ),
    )
    assert(fitted(0) > fitted(1) && fitted(1) > fitted(2))

  test("Players that have only drawn are fitted equal abilities."):
    val drawn  = Seq(Outcome(0, 1, 0.5), Outcome(1, 0, 0.5))
    val fitted = BradleyTerry.fit(2, drawn)
    assertEqualsDouble(fitted(0), fitted(1), 1e-9)

  test("Beating a strong player counts for more than beating a weak one."):
    // Player 1 beats everyone else, so is the strongest of the opposition;
    // player 2 beats it, while player 3 beats only the weakest player.
    val results = Seq(
      Outcome(1, 4),
      Outcome(1, 5),
      Outcome(1, 6),
      Outcome(2, 1),
      Outcome(3, 6),
    )
    val fitted = BradleyTerry.fit(7, results)
    assert(fitted(2) > fitted(3))

  test(
    "A player with no comparisons is fitted the prior's opponent's ability.",
  ):
    val fitted = BradleyTerry.fit(3, Seq(Outcome(0, 1)))
    assertEqualsDouble(fitted(2), 0.0, 1e-6)

  test(
    "An uncompared player sits at the prior's ability whatever the rest do.",
  ):
    val few  = BradleyTerry.fit(3, Seq(Outcome(0, 1)))
    val more = BradleyTerry.fit(4, Seq(Outcome(0, 1), Outcome(0, 2)))
    assertEqualsDouble(few(2), 0.0, 1e-6)
    assertEqualsDouble(more(3), 0.0, 1e-6)

  test("An undefeated player is fitted a finite ability."):
    val swept  = (1 until 5).map(Outcome(0, _))
    val fitted = BradleyTerry.fit(5, swept)
    assert(fitted(0).isFinite && fitted(0) > 0)

  test("The fit is unchanged by the order the comparisons are given in."):
    val results = Seq(
      Outcome(0, 1),
      Outcome(1, 2),
      Outcome(2, 0),
      Outcome(0, 2),
    )
    assertEquals(
      BradleyTerry.fit(3, results),
      BradleyTerry.fit(3, results.reverse),
    )

  test("Comparisons naming a player that does not exist are ignored."):
    assertEquals(
      BradleyTerry.fit(
        2,
        Seq(
          Outcome(0, 1),
          Outcome(0, 7),
          Outcome(1, 1),
        ),
      ),
      BradleyTerry.fit(2, Seq(Outcome(0, 1))),
    )

  test("Repeating a result widens the gap it implies."):
    val once   = BradleyTerry.fit(2, Seq(Outcome(0, 1)))
    val thrice = BradleyTerry.fit(2, Seq.fill(3)(Outcome(0, 1)))
    assert(thrice(0) - thrice(1) > once(0) - once(1))

  test("A field with no comparisons at all is fitted equal abilities."):
    assertEquals(
      BradleyTerry.fit(3, Seq.empty),
      Vector(0.0, 0.0, 0.0),
    )

  test("An Elo lead of 400 points means winning nine comparisons in ten."):
    val lead = BradleyTerry.elo(math.log(10)) - BradleyTerry.elo(0)
    assertEqualsDouble(lead, 400.0, 1e-9)

  test("An average ability sits at the middle of the Elo scale."):
    assertEqualsDouble(BradleyTerry.elo(0), 1500.0, 1e-9)
