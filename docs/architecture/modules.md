# Module boundaries

KinetiFrame has seven modules. This page lists what each may
depend on and why. The [architecture overview](README.md) explains what each module does.

| Module | Allowed project dependencies | Must not contain |
| --- | --- | --- |
| `app` | `core`, `engine`, `data`, `media`, `presentation`, `designsystem` | Nothing other modules need |
| `core` | none | Android, Compose, or any other project module |
| `engine` | `core` | Compose, app UI, storage. Math packages: no Android at all |
| `data` | `core` | Compose, `engine`, `presentation`, `designsystem` |
| `media` | `core`, `presentation`, `designsystem` | `data`, `engine`, studio or actor state |
| `presentation` | `core`, `engine`, `designsystem` | `data` implementations |
| `designsystem` | none | Projects, actors, ViewModels, other project modules |

`./gradlew checkArchitecture` enforces these rules by reading imports and fully qualified names
in every Kotlin file. It also rejects provider SDKs (Firebase, Play Billing, Appwrite) in shared
modules.

## Why these boundaries

- **`core` is plain Kotlin** so models and codecs can be tested quickly on any JVM and could be
  reused by desktop or command-line tools that read KinetiFrame files.
- **Engine math has no Android types** so rigging and deformation are deterministic and testable
  without a device. Platform rendering adapters may use Android graphics, never Compose.
- **`designsystem` knows nothing about animation** so UI contributors can work on controls with
  previews and fake data, without loading a project or an actor.
- **`app` wires everything together.** Concrete repositories and platform adapters are created
  there and passed down, so no shared module reaches for a global service.

## Adding a module

Prefer a new package inside an existing module. Don't create a module per screen, model or
feature. A new module needs an [architecture decision record](decisions) that shows the benefit,
the new dependency graph and the build cost, and it needs maintainer approval.

A module's build file applies one of the convention plugins in `buildSrc/src/main/kotlin`, which
set the SDK levels, Java version and test setup every module shares:

| Plugin | For |
| --- | --- |
| `kinetiframe.jvm.library` | Plain Kotlin modules such as `core` |
| `kinetiframe.android.library` | Android library modules |
| `kinetiframe.android.application` | The app |
| `kinetiframe.android.compose` | Added to either Android plugin for modules with Compose UI |

The build file then only names its namespace and its dependencies.

## Moving code between modules

Most product code still lives in `app`. When moving a piece into its target module:

- Move one workflow at a time and keep the build and tests passing after each step.
- Keep serialized names, defaults and actor revision IDs exactly as they are.
- Keep the same cache and bitmap lifetimes. Change ownership in a separate pull request.
- Don't pull an `app` ViewModel or repository into a library just to move a screen file. Move the
  screen when its dependencies can move with it.
- Run the owning tests, `checkArchitecture` and the app build.
