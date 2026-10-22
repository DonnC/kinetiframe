# Code guidelines

These guidelines keep KinetiFrame easy to read, safe to change and pleasant to animate with. The
automated checks enforce some of them. Reviewers look for the rest.

If a guideline gets in the way of doing the right thing, say so in your pull request. Guidelines
change through discussion; see [governance](../GOVERNANCE.md).

## The animator comes first

- Architecture is never a reason to make the studio worse. Keep the drawing area large, gestures
  direct and controls reachable.
- Prefer direct manipulation over dialogs with number fields. Show only the tools that apply to
  what the user is doing.
- Every icon button needs a localized content description. Decorative images have none.
- Test touch, stylus, playback and export changes on a real device.

## Size and shape

- **Files stay under 500 lines**, counting comments and blank lines. Split by responsibility,
  not by moving code into an unrelated "utils" file.
- **Functions and lambdas take at most 6 parameters; constructors at most 7.** Data class
  constructors are exempt. In `@Composable` functions only parameters without a default value
  count, so optional `modifier` and styling parameters are free.
- When a function needs more inputs, group values that belong together into a small, well-named
  type. Don't bundle unrelated things into a catch-all `Options`, `Context` or `Dependencies`
  object just to pass the check.
- **No god objects.** A class that coordinates unrelated workflows is hard to test and harder to
  change, however many lines it has. Each piece of mutable state or resource has one owner.
- **One screen, route, dialog or reusable control per file.** Small private helpers and previews
  can stay with it. No `Components.kt` or `Common.kt` grab bags.

`./gradlew checkArchitecture` checks file size, parameter counts and module imports. Older code
that breaks these rules is listed in `.architecture/legacy-baseline.properties`. That list can
only shrink. If you touch a file on it, don't add new violations, and fix the existing one if
the change is small enough.

## Modules and dependencies

| Module | May depend on |
| --- | --- |
| `app` | every other production module |
| `presentation` | `core`, `engine`, `designsystem` |
| `engine` | `core` |
| `data` | `core` |
| `core` | nothing (plain Kotlin, no Android) |
| `designsystem` | nothing in the project (Compose only) |
| `sandbox` | `core`, `engine` |

- Dependencies point down this table. No cycles.
- Engine math packages use no Android or Compose types. Translate geometry at the UI edge.
- Prefer a package over a new module. A new module needs an
  [architecture decision record](architecture/decisions) and maintainer approval.
- Put storage, codecs and external services behind small interfaces where a replacement is
  realistic. Don't wrap every helper in an interface, and keep indirection out of hot loops.
- Wire implementations together in `app` at lifecycle boundaries. No global service locator or
  singleton that hands out arbitrary services.

## Packages

- Group code by feature first: `studio`, `actors`, `library`, `project`, `audio` and so on.
  Inside a feature, `ui`, `domain` and `data` subpackages hold the layers it needs. See the
  [project architecture](architecture/decisions/project-architecture.md) decision.
- A package name never repeats a module name. `com.donnclab.kinetiframe.engine` belongs only to
  the `engine` module. `checkArchitecture` rejects `app` files in the old layer packages (`ui`,
  `domain`, `data`, `engine`, `models`, `core`).
- Preview fixtures and fake data live in the `preview` package and are used only by previews and
  tests.

## Naming

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and
  the [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md).
- Name interfaces after what they are (`SnapshotStore`, `AssetCatalog`). Name implementations
  after what makes them different (`FileSnapshotStore`, `RoomActorRepository`), or `Default…`
  when there is only one. Don't prefix interfaces with `I` or suffix classes with `Impl`.
- Use names from the [glossary](glossary.md) so code and app speak the same language.

## State and concurrency

- ViewModels extend `BaseViewModel<State, Intent, Effect>`. The UI sends intents and observes
  immutable state. Use effects only for one-off events such as navigation; anything the user must
  be able to recover belongs in state.
- Use coroutines and `Flow`. Adapt callback APIs at the platform edge.
- Never swallow `CancellationException` or turn it into an error.
- Keep gesture drafts, undo history and saved documents separate. Cancelling or discarding must
  never save a draft.
- Do file and media work off the main thread. Never decode images or allocate bitmaps or paths
  inside a Compose draw phase.
- Take dispatchers as constructor parameters, defaulting to `Dispatchers.IO` or
  `Dispatchers.Default`, instead of naming them inline. Tests pass a test dispatcher so every
  coroutine they start also finishes inside the test.
- Give caches and bitmap pools a size limit and an owner who clears them.

## Saved files must keep opening

People's animations are stored in the formats described in [file formats](file-formats.md).
Breaking them loses someone's work.

- Don't rename serialized fields, enum wire names or `@SerialName` values. Never persist enum
  ordinals.
- Some serial names still use the project's old `com.kinetiframe` package prefix. They are
  file-format identifiers, not code packages. Leave them as they are.
- Every format change needs a compatibility test that loads the old format, and a plan for files
  that can't be migrated.
- Validate imported files before changing anything on disk. Reject unsafe archive paths, missing
  files and impossible sizes with a clear message.
- Don't change a format in the same pull request as a refactor.

## Types and values

- Use enums for small fixed sets of choices and sealed types when choices carry different data.
  IDs and user-defined values are not enums.
- No magic values. User-facing text goes in Android string resources. Shared colors and sizes go
  in `designsystem` tokens. File names, keys and limits are named constants next to the code that
  owns them, not in a global `Constants` object.
- `core` and `engine` report errors as typed values, never as Android resource IDs or English
  sentences.
- Default to `private` or `internal`. Avoid `Any`, unchecked casts and string-keyed maps at
  module boundaries.
- Import each symbol explicitly. No wildcard imports, and no fully qualified names in code bodies
  unless two names clash.

## Reuse

- When two pieces of code do almost the same thing, extract one implementation with explicit
  options for the differences. Don't merge things that only look alike: drawing and actor tools
  often have different rules on purpose.
- Prefer composition to inheritance. Inherit only when the child really is a substitute for the
  parent.

## Compose

- Separate routes (which collect state and talk to ViewModels) from stateless screen content.
- Reusable controls take values and callbacks. They don't fetch ViewModels, read repositories or
  load files.
- Provide `@Preview`s with fake data for every reusable control, including empty, disabled and
  error states where they apply.

## Dependencies

- Prefer the platform or an existing dependency. A new library needs a clear reason, an active
  project, a compatible license and a look at its size and transitive dependencies.
- Pin exact versions. Don't upgrade dependencies in an unrelated pull request.
- No proprietary SDKs, analytics, ads or tracking.

## Tests

- Fix a bug by first writing a test that fails because of it.
- `core` and engine math tests use JUnit 5 with no Android, network or disk.
- Storage tests use temporary real files. External services are replaced with fakes.
- Run the same contract tests against fakes and real implementations.
- Cover the unhappy paths: cancellation, missing permissions, partial writes, bad input.
- Compiling and passing host tests doesn't prove a gesture or render works. Check on a device.

## Comments

Good names and small functions explain *what* the code does. Comments explain *why*:

- an invariant the code relies on
- a compatibility constraint, such as an old file format quirk
- a platform workaround, and when it can be removed
- non-obvious math, with a reference if there is one

A comment saying legacy loops clamp negative time instead of wrapping explains a constraint. A
comment saying "calculate the time" adds nothing. Keep comments short and factual. Don't leave
development diaries, conversation or commented-out code. Update or delete comments when the
code changes.

New source files start with `// SPDX-License-Identifier: Apache-2.0`.

## Refactoring

- Move one workflow at a time, keeping the build and tests green after each step.
- Don't mix structural moves with behavior, format or UI changes.
- Record important trade-offs in an [architecture decision record](architecture/decisions).
