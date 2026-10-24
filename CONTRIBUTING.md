# Contributing to KinetiFrame

Thanks for your interest in KinetiFrame. You don't need to be an Android developer to help.
Animators, illustrators, designers, writers and translators all make this project better.

Please read the [code of conduct](CODE_OF_CONDUCT.md) before taking part.

## Ways to help

### Use it and tell us what happens

The most useful thing an animator can do is make something with KinetiFrame and tell us where it
got in the way. Install a [development build](https://github.com/DonnC/kinetiframe/releases),
try a short scene, and open an issue for anything confusing, slow or broken.

A good bug report includes:

- the app version or commit, your device model and Android version
- the exact steps, starting from a new project or a bundled example
- what you expected and what happened instead
- a screen recording if the problem involves drawing, touch or playback

Attach a small `.kframe` project if it helps, but only one with no private media in it.

### Draw for the project

Actor kits, stamp packs, backgrounds, brushes and sample scenes are all welcome. Read the
[asset guide](docs/assets.md) first: it explains how characters must be cut and registered so
they bend cleanly, and what source and license information we need for each file.

### Write or translate

The [user guide](docs/user-guide.md) is short and could use screenshots, tutorials and examples.
Translation needs the app's text moved into Android string resources first, which is a good
first task for a developer (see the [roadmap](ROADMAP.md)).

### Write code

Read on.

## Your first code contribution

1. Find an issue labeled
   [`good first issue`](https://github.com/DonnC/kinetiframe/labels/good%20first%20issue) or
   [`help wanted`](https://github.com/DonnC/kinetiframe/labels/help%20wanted). Comment on it so
   others know you are working on it.
2. For anything larger than a bug fix, open an issue or a discussion first and describe what you
   plan to do. This saves you from building something that conflicts with work in progress.
3. Fork the repository and create a branch from `develop`, for example `fix/onion-skin-opacity` or
   `feat/audio-waveform`.
4. Set up your machine with the [development guide](docs/development.md).
5. Make your change, with tests (see below).
6. Run `./gradlew spotlessApply` to format your code, then `./gradlew verify` and fix anything it
   reports.
7. Open a pull request against `develop` and fill in the template.

`develop` collects reviewed changes and publishes development builds. A maintainer merges
`develop` into `main` when it is ready for a release. See [releases](docs/releases.md).

Small fixes, typo corrections and documentation improvements can go straight to a pull request.

## Where code goes

New contributors often ask where something lives. The short version:

| If you are changing... | Look in |
| --- | --- |
| Colors, spacing, typography or a reusable control | [`designsystem`](designsystem/README.md) |
| A screen, editor tool or app workflow | `app`, in its feature package such as `studio`, `actors` or `library` |
| Rigging, IK, deformation or motion math | [`engine`](engine/README.md) |
| Saved document models and their JSON codecs | [`core`](core/README.md) |
| Durable file writes | [`data`](data/README.md) |

The [architecture overview](docs/architecture/README.md) explains the modules and how data
flows between them. The [glossary](docs/glossary.md) explains project terms such as actor,
performance and placement.

## Code guidelines

The full guide is [docs/code-guidelines.md](docs/code-guidelines.md). The points reviewers check
most often:

- **Keep files and functions small.** Source files stay under 500 lines. Functions take at most
  six parameters and constructors seven. Data classes are exempt, and in `@Composable` functions
  only required parameters count. `./gradlew checkArchitecture` enforces this.
- **Respect module boundaries.** `core` has no Android code. Engine math has no Android or Compose
  code. `designsystem` knows nothing about projects or actors. The architecture check enforces
  this too.
- **Follow the MVI pattern.** ViewModels extend `BaseViewModel`, take intents and publish
  immutable state.
- **Never break saved files.** Projects, actors and performances people have already saved must
  keep opening. Don't rename serialized fields or change defaults without a migration and a
  compatibility test.
- **Use explicit imports**, and put user-facing text in Android string resources.
- **Comment the why, not the what.** Explain invariants, workarounds and non-obvious math.
  Don't narrate code that already reads clearly.
- **Keep pull requests focused.** One problem per pull request. Don't mix refactoring with
  behavior changes or dependency upgrades.

## Tests

Add a test that fails without your fix. Engine and core logic use JUnit 5 and must not need
Android, the network or the disk. App code uses JUnit and Robolectric. Compose interactions use
Compose UI tests, which run on a device or emulator.

```sh
./gradlew spotlessApply                        # format Kotlin with ktlint
./gradlew verify                               # formatting, architecture check, host tests, lint
./gradlew :app:connectedDebugAndroidTest       # device tests, needs a phone or emulator
```

Passing host tests is not the same as working on a device. If you changed touch input, drawing,
playback, audio, video or export, try it on real hardware and say what you tested in the pull
request.

## Pull requests

In the pull request description, explain:

- what problem it solves (link the issue)
- how you solved it, including any change to saved files, dependencies or assets
- how you tested it, and what you could not test
- screenshots or a short recording for anything visible

Add a line to the `Unreleased` section of [CHANGELOG.md](CHANGELOG.md) for changes users will
notice. If the pull request starts or finishes a task in the
[restructuring tracker](docs/restructuring.md), or lowers a debt count, update the tracker too.

A maintainer will review your pull request as soon as they can. If you haven't heard back after a
week, leave a comment. Reviews are a conversation, not a verdict: expect questions and
suggestions, and feel free to ask your own.

## Commit messages

Write the subject line in the imperative mood, as if completing "This commit will...":
`Fix onion skin opacity on held frames`, not `Fixed onion skin`. Keep it under about 70
characters. Use the body to explain why the change was needed.

## Working notes

Put personal notes, plans and scratch files in the `notes/` folder at the repository root. Git
ignores it, so your notes stay on your machine and never end up in a pull request. Generated
render output goes in `.artifacts/`, which is also ignored.

## Dependencies

Each new library adds download size, maintenance work and license obligations. Before adding
one, open an issue explaining what it does that the platform or an existing dependency cannot.
Pin exact versions (no `+` ranges). Don't add proprietary SDKs, analytics or ads.

## Licensing

KinetiFrame is licensed under [Apache-2.0](LICENSE). By submitting a contribution you agree that
it is licensed under the same terms. No contributor license agreement is required.

Only submit work you have the right to share. For artwork and audio, record where it came from,
who made it and under what license. If you used an image generator or other tool, say so in the
asset's metadata. Never copy assets from another app or game.

Don't commit credentials, signing keys or anyone's private projects. Report security problems
privately as described in [SECURITY.md](SECURITY.md).

## Questions

Ask in [GitHub Discussions](https://github.com/DonnC/kinetiframe/discussions) or on the issue
you are working on. There are no silly questions, especially about animation terms or about
where something lives in the code.
