# CLAUDE.md

This file provides guidance to [Claude Code](https://claude.com/product/claude-code) when working with code in this repository.
It is not intended for human eyes.

### Maintenance

You (robot or human) have standing permission to update this file without asking.
Add important patterns, gotchas, or context that would help future sessions.
Keep it concise and actionable.

## Project overview

This is Dike, a Scala 3 library for ranking items by judging them in pairs, built on Cats alone. It is in beta.

It is a single JVM module in `com.alecdorrington.dike`. A `Contest` (items, `Anchor`s of known score, round-robin
`rounds`, and the `Curve` to fall back on) is run by `Contest.rank` with a `Judge[F, A]`, `(A, A) => F[Verdict]`, for
any `F` with `MonadThrow` and `Parallel`, giving a `Ranking` of `Rating`s, `Contest.presentedFirst` counts the judgements
sure to present each player first (`Field.certain`: the schedule and each item's first rung, `Field.middle`), and
`Contest.maxJudgements` prices one before
it runs (`Field.maxJudgements`: the schedule and each item's longest climb, `Field.longestClimb`, each judged
`Comparison.sides` times; keep it in step with `Judging` whenever what is judged changes). Internally, a `Field`
indexes the players (the items in the order given, then the anchors in score order), a `Judging` makes the judgements
(the `Field.schedule` of round-robin pairings and adjacent rungs, then each item's binary-search climb, in parallel),
each pairing a `Comparison` judged both ways round, and `Ranking.of` fits `BradleyTerry` abilities and reads scores
off `Calibration`, else normalises each item's standing onto the curve (`Curving.normalised`: a value ranked among a
sample, then the curve read at its percentile). The maths objects (`BradleyTerry`, `RoundRobin`, `Calibration`, `Curving`,
`Gaussian`) are public and usable alone.

- The library never judges. What a judge asks, of whom, and at what cost belongs to the host, and so do the choice of
  items and anchors, limits on how many, and whatever the scores mean. Keep prompts, parsing of a model's answer,
  throttling and retries out.
- Every pairing is judged both ways round (`Comparison`), each judgement weighing `Comparison.judgementWeight` (half a
  comparison); a draw is two quarter-weight outcomes. Never judge a pairing one way only: that is what cancels a
  judge's preference for whatever it saw first.
- A judge's failure, raised in `F` or thrown, must never fail the contest: `Judging.ask` catches both, and the error
  lands in `Ranking.failures`. Whether a ranking with failures is good enough is the host's call.
- Scores are unbounded (`Calibration.score` extrapolates past the ladder, `Curving.normalised` reads a curve with no ends).
  Hosts clamp to their own scales; don't add a range here.
- Indices in `BradleyTerry.Outcome` are `Field` player indices; they never leave the library. The public API speaks in
  items, and `Ranking.ratings` keeps the items' order so that hosts can zip their own keys back on.

See [README.md](README.md) for usage.

### Where this code lives

This repository is a mirror. The library is developed inside a larger private project, beneath `dike/`, and every file
here is copied from there by [GitHub Graph](https://github.com/SgtSwagrid/github-graph) whenever that project's `main`
changes, overwriting whatever is here. So make changes there, never here. The shared configuration (workflows, Scalafmt, IDE settings, `project/plugins-*.sbt`) comes from further upstream still, in
[Scala Library Config](https://github.com/SgtSwagrid/scala-library-config), which syncs into the private project's `dike/` first.
`build.sbt`, `release.sbt`, `project/Dependencies.scala`, `README.md` and this file belong to the library.

### Build

- The root project `dike` is the library itself, published as `dike`. Its id is the library's name because the private
  project includes this build by reference (`ProjectRef(file("dike"), "dike")`), alongside projects of its own.
- The library must never depend on anything in the project that includes it.
- Cats Effect is a test dependency only: the suites judge in `IO` (`munit-cats-effect`), but the library asks nothing
  of `F` beyond `MonadThrow` and `Parallel`.
- Versions come from git tags (`sbt-ci-release`); publishing a GitHub release publishes to Maven Central.

## Instructions

### Compilation and Diagnostics

- When the user asks for help with a compilation or type error, start by running `sbt compile` to see the error for yourself.
  If there are many errors, making it unclear which one the user is referring to, ask them to clarify, and then focus only on that issue.
- IntelliJ MCP integration is active. When a request seems to implicitly refer to something the user is looking at, always check
  `mcp__ide__getDiagnostics` first to see which file(s) are open and get associated diagnostics (errors, warnings, and info hints with line numbers).

#### Testing

- After making code changes, always run `sbt compile` to verify that issues are fixed and no new ones are introduced.
- Repeatedly retry upon failure until the build succeeds. If you are unsure how to fix an issue, ask for help or refer to existing code for examples.
- Before trying to fix an error, make sure you first understand it fully.
- You should never report that a feature is complete without testing it first.

### Code Style

- You must read the [Code Style Guidelines](docs/STYLE_GUIDE.md).
- Document every public type and member: a summary, then `@param` for each explicit parameter,
  `@tparam` for each type parameter, and `@return` for any result but `Unit`, each one short
  sentence. Summaries read: types "A ...", values "The ...", Booleans "Whether ...", methods a
  third-person verb ("Sends ..."), never "Returns ...". Private members get a comment only for a
  non-obvious contract or gotcha, usually in one sentence.

### Pull Requests

When asked to publish the code changes, your task is to open one or more pull requests (PRs) to merge the changes into `main` on GitHub:

- Use `git` to check what has changed as compared to the `main` branch on `origin`.
- If the changes are thematically linked, they can be published as a single PR.
- Otherwise, you'll need to divide the changes into multiple PRs using your own judgement.
- Each PR should have a singular focus, shouldn't break anything, and should be able to be merged independently.
- Ensure that all code is staged, committed and pushed. Ensure no new files are left uncommitted, and no debug code is left in the codebase.
- When creating a PR, ensure that the title and description are clear, informative, and comprehensive.
- All feature/bugfix/etc branch names should be formatted as "feature_<short description>" or "fix_<short description>" or similar.
- All PR titles should be formatted as "[<scope>] <Short summary>", e.g. "[renderer] Fixed colour inversion bug."
- You have GitHub MCP integration that can be used to do the above.
