package com.alecdorrington.dike

import com.alecdorrington.dike.BradleyTerry.Outcome
import munit.FunSuite

final class BradleyTerrySuite extends FunSuite:

  test("a player that beats another is fitted the stronger of the two"):
    val fitted = BradleyTerry.abilities(2, Seq(Outcome(0, 1)))
    assert(fitted(0) > fitted(1))

  test("a transitive run of results is fitted in its own order"):
    val fitted = BradleyTerry.abilities(
      3,
      Seq(
        Outcome(0, 1),
        Outcome(1, 2),
        Outcome(0, 2),
      ),
    )
    assert(fitted(0) > fitted(1) && fitted(1) > fitted(2))

  test("players that have only drawn are fitted equal abilities"):
    val tied   = Seq(Outcome(0, 1, 0.5), Outcome(1, 0, 0.5))
    val fitted = BradleyTerry.abilities(2, tied)
    assertEqualsDouble(fitted(0), fitted(1), 1e-9)

  test("beating a strong player counts for more than beating a weak one"):
    // Player 2 beats the strongest opponent, player 3 only the weakest.
    val results = Seq(
      Outcome(1, 4),
      Outcome(1, 5),
      Outcome(1, 6),
      Outcome(2, 1),
      Outcome(3, 6),
    )
    val fitted = BradleyTerry.abilities(7, results)
    assert(fitted(2) > fitted(3))

  test("a player with no comparisons is fitted the prior's opponent's ability"):
    val fitted = BradleyTerry.abilities(3, Seq(Outcome(0, 1)))
    assertEqualsDouble(fitted(2), 0.0, 1e-6)

  test("an uncompared player sits at the prior's ability whatever the rest do"):
    val few  = BradleyTerry.abilities(3, Seq(Outcome(0, 1)))
    val more = BradleyTerry.abilities(4, Seq(Outcome(0, 1), Outcome(0, 2)))
    assertEqualsDouble(few(2), 0.0, 1e-6)
    assertEqualsDouble(more(3), 0.0, 1e-6)

  test("an undefeated player is fitted a finite ability"):
    val swept  = (1 until 5).map(Outcome(0, _))
    val fitted = BradleyTerry.abilities(5, swept)
    assert(fitted(0).isFinite && fitted(0) > 0)

  test("the fit is unchanged by the order the comparisons are given in"):
    val results = Seq(
      Outcome(0, 1),
      Outcome(1, 2),
      Outcome(2, 0),
      Outcome(0, 2),
    )
    assertEquals(
      BradleyTerry.abilities(3, results),
      BradleyTerry.abilities(3, results.reverse),
    )

  test("comparisons naming a player that does not exist are ignored"):
    assertEquals(
      BradleyTerry.abilities(
        2,
        Seq(
          Outcome(0, 1),
          Outcome(0, 7),
          Outcome(1, 1),
        ),
      ),
      BradleyTerry.abilities(2, Seq(Outcome(0, 1))),
    )

  test("repeating a result widens the gap it implies"):
    val once   = BradleyTerry.abilities(2, Seq(Outcome(0, 1)))
    val thrice = BradleyTerry.abilities(2, Seq.fill(3)(Outcome(0, 1)))
    assert(thrice(0) - thrice(1) > once(0) - once(1))

  test("a field with no comparisons at all is fitted equal abilities"):
    assertEquals(
      BradleyTerry.abilities(3, Seq.empty),
      Vector(0.0, 0.0, 0.0),
    )

  test("an Elo lead of 400 points means winning nine comparisons in ten"):
    val lead = BradleyTerry.elo(math.log(10)) - BradleyTerry.elo(0)
    assertEqualsDouble(lead, 400.0, 1e-9)

  test("an average ability sits at the middle of the Elo scale"):
    assertEqualsDouble(BradleyTerry.elo(0), 1500.0, 1e-9)
