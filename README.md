<div align="center">

  <h1>⚖️ Dike</h1>
  <p>Ranking by pairwise comparison in <a href="https://www.scala-lang.org/">Scala</a>, with any judge, from a language model to a match.</p>

  <span>
    <a href="https://github.com/SgtSwagrid/dike/actions/workflows/build-integrity.yml"><img src="https://github.com/SgtSwagrid/dike/actions/workflows/build-integrity.yml/badge.svg" alt="Build status" /></a>
    <a href="https://search.maven.org/artifact/com.alecdorrington/dike_3"><img src="https://img.shields.io/maven-central/v/com.alecdorrington/dike_3.svg" alt="Maven Central" /></a>
    <a href="https://alecdorrington.com/dike"><img src="https://img.shields.io/badge/docs-latest-blue.svg" alt="Documentation" /></a>
  </span>

</div>

> [!WARNING]
> Dike is in beta. It is young, it has one user, and anything may change between minor versions.

Ask a judge what something is worth and the answer drifts. Ask it which of two things is the better and the answer holds.
This is as true of a large language model as of a person, so a ranking fitted from many such judgements is far
steadier than the scores it replaces. Dike does everything but the judging: it chooses which pairs to compare,
asks both ways round to cancel out any bias for what came first, fits an ability to every item,
and calibrates the result against items whose scores you already know.
You supply the judge, as a function in whatever effect you like.
It is built on [Cats](https://typelevel.org/cats/), and has no other dependencies.

Named for [Dike](https://en.wikipedia.org/wiki/Dike_(mythology)), goddess of justice and of fair judgement.

## ⬇️ Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "com.alecdorrington" %% "dike" % "0.1.0"
```

Compiled with Scala `3.9.0`, with no intention to explicitly support older versions. JVM only.

## 🚀 Usage

Describe what to rank as a [`Contest`](src/main/scala/Contest.scala), then run it with a [`Judge`](src/main/scala/Judge.scala):
a function from two items, in the order they are presented, to the [`Verdict`](src/main/scala/Verdict.scala) on which is better.

```scala
import cats.effect.IO
import com.alecdorrington.dike.{Anchor, Contest, Judge, Verdict}

val judge: Judge[IO, Book] = (first, second) => ??? // Verdict.First, Verdict.Second or Verdict.Tie.

val ranking = Contest(
  items = books,
  anchors = List(Anchor(dull, 2), Anchor(decent, 5), Anchor(classic, 9)),
  rounds = 2,
).rank(judge)
```

The result is a [`Ranking`](src/main/scala/Ranking.scala): one [`Rating`](src/main/scala/Ranking.scala) per item,
in the order you gave them (or strongest first, with `ranked`), each with

- a `score`, on the same scale as the anchors' scores,
- an `elo`, the fitted ability on the [Elo](https://en.wikipedia.org/wiki/Elo_rating_system) scale, where a lead of 400 points means winning nine comparisons in ten,
- and its `wins`, counted in comparisons.

It also says how many `judgements` the fit rests on, the `failures` of any which were left out, and the `agreement`
of the anchors' own scores with the order the judge put them in. A low agreement means the anchors are scored inconsistently, or that the judge
is not judging what their scores measure; either way, the scores should not be trusted.

A judge may be anything which decides between two items: a person, a match played between them, or a language model:

```scala
import com.alecdorrington.iris.{LlmClient, Prompt}

def judge(client: LlmClient[IO]): Judge[IO, String] = (first, second) =>
  client
    .complete(Prompt(s"Which blurb makes you want to read the book more?\n\nA: $first\n\nB: $second\n\nAnswer A, B or TIE."))
    .map(_.text.trim match
      case "A" => Verdict.First
      case "B" => Verdict.Second
      case _   => Verdict.Tie)
```

Judgements are made in parallel wherever none waits on another's verdict, so a judge which costs something per call
should limit for itself how many run at once. Such a judge can also be priced before the contest runs: `maxJudgements`
says the most judgements a contest may ask for, which it reaches only when every item climbs the ladder as far as it can.
A judge which keeps what it reads of the item presented first, as a language model's prompt cache does, can ask
`presentedFirst` how many judgements are sure to present each item and anchor first, and so which it will read again.
A judgement which fails, whether in `F` or by throwing, is left out
of the ranking and reported in its `failures`, rather than failing the whole contest.

## 🧭 How it works

1. **Both ways round.** Every pairing is judged twice, once with each item presented first.
   Agreeing verdicts are a win; contradictory ones are a draw, which is the signal that two items are too alike to tell apart.
   A judge which simply prefers whatever it sees first therefore favours nothing.
2. **Round robin.** The items are paired with one another over `rounds` rounds of a [round-robin](src/main/scala/RoundRobin.scala)
   (circle method), so that every item meets a different opponent each round, and comparisons are spread evenly
   rather than leaving some items barely compared. `rounds = 0` skips this.
3. **The ladder.** The anchors are sorted by score into a ladder, each rung compared with the next to settle the ladder's
   own order. Each item then climbs the ladder by binary search, so placing it costs only a handful of comparisons,
   however long the ladder is.
4. **The fit.** All of these comparisons are fitted together as one body of evidence by the
   [Bradley-Terry](src/main/scala/BradleyTerry.scala) model, by maximum likelihood (Hunter's MM algorithm),
   with a weak prior of imagined draws, so that undefeated items and disconnected fields stay finite.
5. **Calibration.** Each item's score is read off the line between the anchors it fell among, extrapolated past the
   ends of the ladder. With fewer than two anchors, which cannot say what any ability is worth, each item is instead
   scored by its standing among the others, read off the contest's normal `curve`.
   So an anchored field of uniformly strong items is free to score uniformly well, whereas a curve forces every field
   into the same shape.

Scores are not bounded: clamp them to your scale's range if it has one.
Each step is also available on its own, as [`BradleyTerry`](src/main/scala/BradleyTerry.scala),
[`RoundRobin`](src/main/scala/RoundRobin.scala), [`Calibration`](src/main/scala/Calibration.scala),
[`Curving`](src/main/scala/Curving.scala) and [`Gaussian`](src/main/scala/Gaussian.scala).
`Curving.normalised`, for one, places any value on a curve by its standing within a sample, as the contest places its items.

## 🤝 Contributing

Dike is developed as part of a larger private project, of which this repository is an automatically synchronised
mirror (by [GitHub Graph](https://github.com/SgtSwagrid/github-graph)), so changes made here directly would be overwritten.
Issues are very welcome; for anything more, please open an issue first.

## 👁️ See also

- [Iris](https://github.com/SgtSwagrid/iris), a sibling, a provider-agnostic client for large language models, and so for judges.
- [Hecate](https://github.com/SgtSwagrid/hecate), a sibling, for user accounts, sessions, groups and permissions.
- [Eunomia](https://github.com/SgtSwagrid/eunomia), a sibling, for filtering, ordering and paging lists.
- [qr4s](https://github.com/SgtSwagrid/qr4s), a sibling, for generating QR codes, on the JVM and in the browser.
- This library was made using [Scala Library Template](https://github.com/SgtSwagrid/scala-library-template).
