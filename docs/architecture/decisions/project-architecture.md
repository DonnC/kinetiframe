# Project architecture

Accepted by the maintainer on 22 October 2026.

## Context

Most product code lives in `app`, organised by technical layer (`ui`, `domain`, `data`,
`engine`, `models`) instead of by feature. Several of those packages have the same names as
modules: `app` has `com.donnclab.kinetiframe.engine.rigging.math`, and so does the `engine`
module. A reader can't tell from a package name which module a class is in, and the same package
split across two modules is easy to break.

The editor's main types are named `Timeline*` (`StudioViewModel`, `StudioState`,
`StudioIntent`), although they cover the whole studio, not just the timeline.

ViewModels call `Dispatchers.IO` directly. Tests can't control that work, and work that outlives a
test reports its failure in a later test, which makes the host test suite flaky.

## Decision

### Layers

Each screen follows the same layers, top to bottom. A layer only calls the one below it.

1. **Compose UI.** A route composable gets the ViewModel and collects its state. The screen
   content below it is stateless, takes values and callbacks, and has previews.
2. **ViewModel.** Extends `BaseViewModel` and follows strict MVI: intents in, immutable state and
   one-off effects out. It is the only place screen state changes.
3. **Domain.** Use cases and managers that hold the rules of the product, such as drawing,
   timing and actor posing. No Compose types.
4. **Repositories.** Interfaces for anything stored or loaded. Where a replacement is realistic,
   the interface lives in `core` and the implementation in `app` or `data`.
5. **Data sources.** Files, Room and Android media APIs.

### Modules

Modules stay as described in [module boundaries](../modules.md): `core`, `engine`, `data`,
`presentation`, `designsystem`, `app` and `sandbox`. Code moves out of `app` into a module when
everything it depends on can move with it.

### Packages

Inside each module, packages are grouped by feature first. Within a feature, `ui`, `domain` and
`data` subpackages hold the layers. A feature only uses the subpackages it needs.

A package name never repeats a module name, so `com.donnclab.kinetiframe.engine` and
`com.donnclab.kinetiframe.core` belong only to those modules.

The `app` packages become:

| Package | Holds | Comes from |
| --- | --- | --- |
| `studio` | The frame-by-frame editor: `EditorScreen`, `StudioViewModel` and its MVI types; now `presentation.studio` | `ui` |
| `studio.ui.*` | Canvas, dock, timeline, camera, colour picker, frame strip and editor dialogs; now `presentation.studio.*` | `ui/components/*`, `ui/color` |
| `studio.domain` | Drawing, frames, layers, selection, history, playback and view managers | `domain/manager` |
| `studio.render` | Frame composition and caches | `domain/render` |
| `studio.drawing` | Brushes, shapes, rulers, stroke input math, text, bitmap buffers | `engine/*` |
| `studio.camera` | Camera moves and recipes | `engine/camera`, `data/camera` |
| `studio.reference` | Reference images in the studio | `ui/reference` |
| `project.model` | The project document, drawing commands and stage sizes | `engine/model`, `models` |
| `project.data` | Project storage, `.kframe` archives, the Room database | `data/project` |
| `project.ui` | Project list and new-project flow; now `presentation.project` | `ui/projects` |
| `actors.ui` | Actor Studio and actor controls | `ui/actors`, `ui/components/rigging` |
| `actors.domain` | Placements, performances, posing and actor managers | `domain/actor`, `domain/manager` |
| `actors.render` | Actor drawing and thumbnails | `domain/render`, `engine/rigging/render` |
| `actors.rig` | Rig model, math, presets and textures not yet in `engine` | `engine/rigging/*` |
| `actors.data` | Actor and performance storage | `data/actor`, `data` |
| `audio` | Audio decoding, effects, recording, playback and audio UI | `engine/audio`, `data/audio`, `ui/audio` |
| `video` | Imported video clips | `engine/video`, `data/video`, `domain/manager` |
| `media` | Background images and image decoding | `engine/asset`, `data/image` |
| `library` | Library, packs, palettes and bundled content | `ui/library`, `ui/packs`, `data/library`, `data/color`, `data/demo` |
| `export` | Video export | `data/export`, `ui/export` |
| `home`, `settings`, `diagnostics` | Those screens and their storage | `ui/*`, `data/*` |
| `navigation` | Navigation keys and the bottom bar | `ui/navigation`, `ui/components/navigation` |
| `common` | Shared app UI, strings, coroutine helpers and JSON setup | `ui/common`, `ui/components/common`, `core/coroutines` |
| `preview` | Preview fixtures and fake data, used only by previews and tests; now `presentation.preview` | `ui/common/model/PreviewCatalog.kt` |

`MainActivity` and `KinetiFrameApplication` stay in the root package, which the manifest names.

### Naming

- Editor-wide types use **Studio**: `StudioViewModel`, `StudioState`, `StudioIntent`,
  `StudioEffect`. **Timeline** names only the frame and track strip.
- Names follow the [glossary](../../glossary.md) and the [code guidelines](../../code-guidelines.md).

### Dispatchers

ViewModels and managers take the dispatchers they use as constructor parameters, with
`Dispatchers.IO` or `Dispatchers.Default` as the default value. Tests pass a test dispatcher, so
every coroutine a test starts also finishes inside it.

## Consequences

- Package names say where code lives and what feature it serves. New code has one obvious home.
- Moving packages doesn't change saved files: Room tables have explicit names, and the only
  serialised sealed hierarchy (`BaseNode`) pins its `@SerialName` values.
- Paths in `.architecture/legacy-baseline.properties`, the formatter exclusions and the docs
  change with the moves. Entries are renamed, not added.
- `checkArchitecture` rejects new files in the retired `app` packages (`ui`, `domain`, `data`,
  `engine`, `models` and `core`), so the layout can't drift back.
- Moving code from `app` into modules continues one workflow at a time, as before.
- Some composables still load and save files themselves. That work moves into each screen's
  ViewModel as the screen is reworked, so routes only collect state and send intents.
