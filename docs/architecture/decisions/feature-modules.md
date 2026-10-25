# Feature modules

Accepted by the maintainer on 23 October 2026. Its module list and per-module `ui` packages are
replaced by [UI in presentation](ui-in-presentation.md).

## Context

The [project architecture](project-architecture.md) decision grouped `app` by feature, but `app`
is still one Gradle module holding about 57,000 of the project's 62,000 lines of Kotlin:

| Package | Lines | Files |
| --- | --- | --- |
| `studio` | 25,900 | 216 |
| `actors` | 15,300 | 180 |
| `library` | 6,300 | 70 |
| `project` | 2,300 | 33 |
| `audio`, `video`, `media` | 2,800 | 35 |
| everything else | 4,400 | 41 |

This costs us in three ways:

- **Builds are slow.** A one-line edit recompiles the whole module, which takes several minutes
  on a laptop.
- **Boundaries are only conventions.** Nothing stops a screen from reaching into another
  feature's internals, and the features already depend on each other in both directions.
- **Ownership is unclear.** A contributor can't tell which parts of `app` a change can affect.

Today the features import each other both ways. `actors` makes 173 references into `studio`,
and `studio` imports 93 names from `actors`. The project document model in `project.model` is
used by every feature, yet it imports camera, audio, video and actor types back from them.
Gradle rejects cycles between modules, so these packages can't become modules as they are.

The `data` and `presentation` modules exist but hold under 50 lines each.

## Decision

### `app` is only the entry point

`app` holds `KinetiFrameApplication`, `MainActivity`, the navigation graph and the wiring that
creates repositories and passes them to the features. It holds no screens, managers or storage
of its own.

Features don't navigate. A screen reports what the animator asked for through callbacks, such as
`onOpenProject`, and `app` decides where that leads. Navigation keys live in `app`.

### Target modules

Each module may depend only on modules above it in this list.

| Module | Holds |
| --- | --- |
| `core` | The project document model and its codecs: plain Kotlin, no Android |
| `engine` | Drawing, rigging and deformation math, as today |
| `designsystem` | Theme, tokens and shared controls, as today |
| `data` | Room, file stores, `.kframe` and `.kpack` archives, and crash reports |
| `presentation` | `BaseViewModel`, MVI types and the shared app UI now in `common` |
| `media` | Images, sound and video clips: decoding, playback, recording and their controls |
| `actors` | Rig model, posing, actor rendering and actor storage |
| `library` | The library, packs and palettes |
| `export` | Video export |
| `studio` | The editor, including the parts of Actor Studio that run on `StudioState` |
| `home` | The home screen and the bottom bar |
| `settings` | Settings and app preferences |
| `app` | The entry point described above |

Features are grouped into modules by what changes together, not one module per package. Audio,
video and images share decoding and playback code, so they form one `media` module. Crash
reports are a small store, so they go in `data`.

A module's Kotlin packages keep their feature names, such as `com.donnclab.kinetiframe.studio`
in the `studio` module. The project document model moves from `project.model` to `core`, with
every serialized name kept as it is.

Each module keeps its own preview fixtures in a `preview` package, used only by its previews and
tests. The shared `preview` package in `app` is split up as features move.

### The UI can be replaced on its own

Changing how a screen looks must not touch managers, storage or rendering. Inside every module:

- Only `ui` packages use Compose. Domain, data, model and render code don't import it, so the
  rules of the product never depend on how they're drawn.
- A screen is a route plus stateless content. The route collects the ViewModel's state and sends
  intents. The content takes values and callbacks, and has previews.
- `ui` code never reads a repository, file or database directly. It goes through the ViewModel.
- Controls used by more than one screen are stateless and live in `designsystem` when they know
  nothing about animation, or in `presentation` when they do. Features build their screens from
  them instead of copying them.

Today 27 files outside `ui` packages import Compose, and 44 `ui` files import a `data` package
directly. `checkArchitecture` gets a rule for each, with a baseline that only shrinks.

### Layering comes first

Until a package can move, `checkArchitecture` enforces the same order on packages inside `app`.
A feature may use the features below it and never one above it. The allowed order is defined
in `buildSrc/src/main/kotlin/quality/FeatureLayering.kt`. Today's upward references are listed
in `.architecture/layering-baseline.properties`, and each file's count may only go down.

A package moves into its module once it has no upward references. The move itself is then a
mechanical change, with no edits to how the code works.

### Order of work

1. **Model.** Move the document's pure data types into `core`. Anything with behaviour that the
   model points to today, such as the video frame provider or the waveform extractor, gets
   referenced the other way round instead.
2. **UI rules.** Add the two UI checks above, with their baselines.
3. **Build logic.** Add Gradle convention plugins, so each new module is a few lines of build
   configuration rather than a copy of another module's.
4. **Data and presentation.** Move storage and crash reports into `data`, and shared UI into
   `presentation`.
5. **Media and export.** Extract them. They have few upward references.
6. **Actors.** Split the actor domain from Actor Studio's editor screens. The domain moves into
   `actors`. The screens that use `StudioState` move under `studio`.
7. **Library, then studio.** Extract them once their references only point down.
8. **Home and settings.** Extract them, and move navigation into `app`. `app` is then only the
   entry point.

Each step is its own pull request, and the build and tests pass after each one.

## Consequences

- An edit to a feature recompiles that module and the modules that depend on it, not all of
  the app.
- The module graph enforces boundaries, so no rule has to be checked by review alone.
- The project grows from 7 modules to 13. Each module adds a little configuration time, which
  convention plugins keep small.
- Moving the model changes package names, so the pull request that does it touches many
  imports. Saved files are not affected, because serialized names are pinned.
- Actor Studio's screens end up in `studio` rather than in `actors`, which follows how it works
  today: Actor Studio is the editor with an actor loaded.
