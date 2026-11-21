# UI in presentation

Accepted by the maintainer on 25 October 2026. Replaces the module list and the per-module `ui`
packages of the [feature modules](feature-modules.md) decision. Its other parts still apply:
`app` is only the entry point, features don't navigate, and layering comes first.

## Context

The feature modules decision gave each feature its own module with its own `ui` package, for 13
modules in all. Steps 1 to 6 of that plan are done. `core`, `engine`, `data`, `media`, `actors`,
`presentation` and `designsystem` are modules, and `app` still holds the studio, the library,
export, home, settings and navigation.

Two things argue against finishing it as written:

- **The UI would stay spread out.** With screens in every feature module, changing how the app
  looks means working across many modules, each with its own Compose setup.
- **Most of the remaining modules would be small.** Export is 1,000 lines, home 1,700 and
  settings 300. Each module adds build configuration and a boundary to maintain, and these would
  add little that package rules don't already give.

## Decision

### All UI lives in `presentation`

Screens, routes, stateless content, components and their ViewModels live in `presentation`,
grouped by feature: `presentation.home`, `presentation.studio`, `presentation.actors` and so on.
Changing the UI means working in one module.

`presentation` doesn't import `data`. A screen reaches storage through its ViewModel, and the
ViewModel through the logic modules. A ViewModel may use a logic module's repositories, such as
`ActorRepository` in `actors.data`, but not the `data` module's Room tables, file stores or
project storage underneath them. Screens, routes and components import no `data` package at all.

### Logic modules hold no screens

`media`, `actors` and `studio` hold business logic: managers, services, repositories, rendering
and rules. They don't contain Compose screens. The Compose they use today for geometry and
drawing helpers is counted by the `compose` check and removed over time.

### Nine modules

| Module | Holds |
| --- | --- |
| `core` | The project document model and its codecs: plain Kotlin, no Android |
| `engine` | Drawing, rigging, deformation and camera math, and shared render caches |
| `designsystem` | Theme, tokens and controls that know nothing about animation |
| `data` | Room, file stores, project storage and crash reports |
| `media` | Decoding, mixing, recording and playback of images, sound and video |
| `actors` | Rig model, posing, performances, actor rendering and actor storage |
| `studio` | The editor's managers, rendering, drawing tools, camera, export and the library |
| `presentation` | Every screen and component, their ViewModels, and `BaseViewModel` |
| `app` | `MainActivity`, the navigation graph and the wiring that creates the services |

Export and the library are packages in `studio`, not modules. Export draws each frame with the
studio's compositor, and the studio uses the library as its asset picker, so each changes with
the studio. Home and settings are screens with little logic of their own: their screens go to
`presentation`, and the logic they have goes to the module it belongs with.

A new module needs a reason that a package rule in `checkArchitecture` can't give, such as a
dependency that must be impossible rather than discouraged.

## Consequences

- The UI can be restyled or rebuilt in one module, against previews and fake state.
- `presentation` becomes the largest UI module. Its packages follow the features, and its
  ViewModels are the only way screens reach the logic modules.
- Moving a screen into `presentation` first requires its storage access to go through its
  ViewModel, so the 144 places where UI imports `data` are paid down as screens move.
- The project ends with 9 modules instead of 13.
