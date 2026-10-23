# Feature modules

Proposed on 23 October 2026. Waiting for maintainer approval.

## Context

The [project architecture](project-architecture.md) decision grouped `app` by feature, but `app`
is still one Gradle module holding about 57,000 of the project's 62,000 lines of Kotlin:

| Package | Lines | Files |
| --- | --- | --- |
| `studio` | 25,900 | 216 |
| `actors` | 15,300 | 180 |
| `library` | 6,300 | 70 |
| `project` | 2,300 | 33 |
| `audio` | 2,200 | 22 |
| everything else | 5,500 | 55 |

This costs us in three ways:

- **Builds are slow.** A one-line edit recompiles the whole module, which takes several minutes
  on a laptop.
- **Boundaries are only conventions.** Nothing stops a screen from reaching into another
  feature's internals, and the features already depend on each other in both directions.
- **Ownership is unclear.** A contributor can't tell which parts of `app` a change can affect.

Today's feature references run both ways. `actors` makes 173 references into `studio`, and
`studio` makes 93 into `actors`. The project document model in `project.model` is used by every
feature, yet it imports camera, audio, video and actor types back from them. Gradle rejects
cycles between modules, so these packages can't become modules as they are.

The `data` and `presentation` modules exist but hold under 50 lines each.

## Decision

### Target modules

The app becomes these modules. Each one may depend only on modules lower in the list.

| Module | Holds |
| --- | --- |
| `core` | The project document model and its codecs: plain Kotlin, no Android |
| `engine` | Drawing, rigging and deformation math, as today |
| `designsystem` | Theme, tokens and shared controls, as today |
| `data` | Room, file stores and `.kframe`/`.kpack` archives |
| `presentation` | `BaseViewModel`, MVI types and the shared app UI now in `common` |
| `media`, `audio`, `video` | Images, sound and video clips: decoding, playback and their controls |
| `actors` | Rig model, posing, actor rendering and actor storage |
| `library`, `export` | The library and packs, and video export |
| `studio` | The editor, including the parts of Actor Studio that run on `StudioState` |
| `app` | `MainActivity`, navigation, settings, home and the wiring between modules |

A module's Kotlin package keeps its feature name, for example `com.donnclab.kinetiframe.studio`
in the `studio` module. The project document model moves from `project.model` to `core`, with
every serialized name kept as it is.

Settings, home and diagnostics stay in `app`: they are small, and they sit at the top of the
graph anyway.

### Layering comes first

Until a package can move, `checkArchitecture` enforces the same order on packages inside `app`.
A feature may use the features below it and never one above it. The allowed order is defined
in `buildSrc/src/main/kotlin/quality/FeatureLayering.kt`. Today's 267 upward references are
listed in `.architecture/layering-baseline.properties`, and each file's count may only go down.

A package moves into its module once it has no upward references. The move itself is then a
mechanical change, with no edits to how the code works.

### Order of work

1. **Model.** Move the document's pure data types into `core`. Anything with behaviour that the
   model points to today, such as the video frame provider or the waveform extractor, gets
   referenced the other way round instead.
2. **Build logic.** Add Gradle convention plugins, so each new module is a few lines of build
   configuration rather than a copy of another module's.
3. **Leaf features.** Extract `media`, `audio`, `video` and `export`. They have few upward
   references.
4. **Actors.** Split the actor domain from Actor Studio's editor screens. The domain moves into
   `actors`. The screens that use `StudioState` move under `studio`.
5. **Library, then studio.** Extract them once their references only point down.
6. **Data and presentation.** Fill `data` with the storage code and `presentation` with the
   shared UI, or fold the modules away if that turns out to be the better fit.

Each step is its own pull request, and the build and tests pass after each one.

## Consequences

- An edit to a leaf feature recompiles that module and the modules above it, not all of `app`.
- The module graph enforces boundaries, so no rule has to be checked by review alone.
- The project grows from 7 modules to about 13. Each module adds a little configuration time,
  which convention plugins keep small.
- Moving the model changes package names, so the pull request that does it touches many
  imports. Saved files are not affected, because serialized names are pinned.
- Actor Studio's screens end up in `studio` rather than in `actors`, which follows how it works
  today: Actor Studio is the editor with an actor loaded.
