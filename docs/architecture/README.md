# Architecture

This page explains how KinetiFrame is put together: the modules, how they depend on each other,
and how data moves through the app when you draw, animate an actor, save and export. Read the
[glossary](../glossary.md) first if terms such as *actor*, *placement* or *performance* are new.

## Modules

```mermaid
flowchart TD
    app["app<br/><small>screens, editor, media, persistence, wiring</small>"]
    presentation["presentation<br/><small>BaseViewModel, MVI</small>"]
    designsystem["designsystem<br/><small>theme, tokens, controls</small>"]
    engine["engine<br/><small>rigging, IK, skinning, motion</small>"]
    data["data<br/><small>durable file writes</small>"]
    core["core<br/><small>models, codecs, validation</small>"]
    sandbox["sandbox<br/><small>engine test app</small>"]

    app --> presentation
    app --> designsystem
    app --> engine
    app --> data
    app --> core
    presentation --> designsystem
    presentation --> engine
    presentation --> core
    engine --> core
    data --> core
    sandbox --> engine
    sandbox --> core
```

| Module | Type | Responsibility |
| --- | --- | --- |
| [`app`](../../app/README.md) | Android app | Entry point, navigation, the studio and other screens, rendering and media adapters, repositories, Room database, bundled assets |
| [`core`](../../core/README.md) | Kotlin/JVM | Platform-neutral records for actors, rigs, motion and presets; JSON codecs; validation; service contracts; the MVI `StateStore` |
| [`engine`](../../engine/README.md) | Android library | IK (FABRIK and two-bone), world transforms, skin deformation, artwork registration, pose and curve sampling, performance clocks, motion presets |
| [`data`](../../data/README.md) | Android library | `DurableFile`: atomic, crash-safe file writes |
| [`presentation`](../../presentation/README.md) | Android library | `BaseViewModel` and shared MVI plumbing |
| [`designsystem`](../../designsystem/README.md) | Android library | Compose theme, design tokens and reusable controls |
| [`sandbox`](../../sandbox/README.md) | Android app | A small app for trying engine changes without the full app |

Dependencies only point downward. `./gradlew checkArchitecture` fails the build if code imports
across a boundary it shouldn't, for example Android types in `core` or Compose in engine math.
[Module boundaries](modules.md) has the full rules.

### Work in progress

Most product code still lives in `app`, organised by feature as described in the
[project architecture](decisions/project-architecture.md) decision. Code moves from `app` into a
library module when everything it depends on can move with it. Package names never repeat a
module name, so `com.donnclab.kinetiframe.engine` is always the `engine` module.

## Where to find things in `app`

All paths are under `app/src/main/java/com/donnclab/kinetiframe/`.

| Area | Package |
| --- | --- |
| Activity and navigation | `MainActivity.kt`, `navigation` |
| Home screen | `home`, `project/ui` |
| The studio (main editor) | `studio/EditorScreen.kt`, `studio/StudioViewModel.kt`, `studio/StudioState.kt`, `studio/StudioIntent.kt` |
| Canvas input and drawing surface | `studio/ui/canvas` |
| Editor toolbars and menus | `studio/ui/dock`, `studio/ui/timeline` |
| Brushes and drawing tools | `studio/drawing` |
| Frame and bitmap caches | `studio/render`, `studio/drawing` |
| Stroke, undo, layer and selection coordination | `studio/domain` |
| Actor Studio | `actors/ui`, `actors/ui/rigging` |
| Actor posing, placements and performances | `actors/domain` |
| Rendering actors | `actors/render`, `actors/rig/texture` |
| Camera effects | `studio/camera` |
| Library | `library/ui`, `library/ui/catalog`, `library/data` |
| Stamp packs | `library/ui/packs`, `library/data/PackArchiver.kt` |
| Projects on disk | `project/data` |
| Actors and performances on disk | `actors/data` |
| Video export | `export/data`, `export/ui` |
| Audio | `audio` |
| Imported video clips | `video` |
| Shared app UI and strings | `common` |

## How the app is structured

Screens follow **Model-View-Intent (MVI)**:

```mermaid
flowchart LR
    UI["Compose screen"] -- "intent (user action)" --> VM["ViewModel<br/>(extends BaseViewModel)"]
    VM -- "immutable state (StateFlow)" --> UI
    VM -- "one-off effect" --> UI
    VM --> Repo["repositories and managers"]
```

- A *route* composable gets the ViewModel and collects its state. The screen content below it is
  stateless and previewable.
- The ViewModel is the only place state changes. It handles every intent explicitly.
- Effects are for things that happen once, such as navigating or showing a message.

The studio's ViewModel is `StudioViewModel`. It
delegates focused jobs to managers, such as `DrawingManager` in `studio/domain` for strokes and
undo, and `RiggingManager` in `actors/domain` for actor editing.

## Drawing a stroke

```mermaid
sequenceDiagram
    participant Input as CanvasInputLayer
    participant VM as StudioViewModel
    participant DM as DrawingManager
    participant Brush as BrushRenderers
    participant Cache as FrameCacheManager
    participant Canvas as BufferedDrawingCanvas

    Input->>VM: touch points (draft stroke)
    Canvas-->>Input: live preview of the draft
    Input->>VM: stroke finished
    VM->>DM: bake stroke
    DM->>Brush: render points with the current brush
    Brush-->>DM: pixels on the layer bitmap
    DM->>Cache: store frame + push undo entry
    Cache-->>Canvas: composed frame for display
```

While your finger is down, the stroke is a draft and is drawn as a preview. Only when it ends is
it *baked* into the layer's bitmap, off the main thread, and recorded for undo. Frames are kept in
a bounded cache in memory and written to the project's `frames/` folder when saved. Cancelling a
gesture throws the draft away without touching saved pixels.

## Animating an actor

```mermaid
flowchart LR
    Rev["Actor revision<br/>(rig + artwork)"] --> Eval
    Perf["Performance<br/>(poses, artwork keys, root motion)"] --> Eval
    Place["Placement<br/>(where and when in the project)"] --> Eval
    Eval["engine: sample pose at time t<br/>IK, presets, skinning"] --> Plan["PuppetRenderPlan<br/>(draw commands)"]
    Plan --> Draw["Canvas / export renderer"]
```

1. An **actor revision** holds the rig (a `SceneGraph` of bones) and the artwork attached to each
   bone. Revisions never change once saved; editing an actor creates a new one.
2. A **performance** stores how the actor moves over time: sampled poses, artwork switches and
   root motion, or an applied motion preset.
3. A **placement** puts one actor revision and one performance into a project at a position and
   time range.
4. For each frame, the engine samples the performance at that moment, solves any IK, and computes
   world transforms and skin deformation. The result depends only on the inputs and the time,
   never on which frame was shown before, so scrubbing and export give identical results.
5. `PuppetRenderPlan` turns the pose into a list of draw commands: an artwork ID, a transform
   matrix and, for continuous drawings, a deformed mesh. The commands hold asset IDs rather than
   bitmaps; the bitmaps themselves are owned by `RigAssetManager`.

## Saving, sharing and exporting

```mermaid
flowchart LR
    Edit["Studio edits"] --> Repo["ProjectRepository"]
    Repo --> Disk["projects/&lt;id&gt;/<br/>manifest.json, document.json,<br/>frames/, performances/, project-assets/"]
    Disk --> Kframe["KframeArchiver<br/>.kframe (ZIP)"]
    Disk --> Export["ProjectExporter<br/>MP4 video"]
    Library["LibraryRepository<br/>library/ + Room index"] --> Pack["PackArchiver<br/>.kpack (ZIP)"]
```

- A project is edited in place as a folder of JSON and PNG files. Writes go through
  `DurableFile`, which writes to a temporary file and swaps it in, so a crash never leaves a
  half-written file.
- A `.kframe` is only created when you share or back up a project. It bundles the project folder
  and every actor it uses. See [file formats](../file-formats.md).
- Library items live under `library/` with a Room database as a searchable index. The files are
  the source of truth.
- Video export renders each frame (drawings, actors, camera move) and encodes it with Android's
  media codecs, together with the audio tracks.

## Design principles

- **Saved work must keep opening.** Serialized names and defaults never change silently. Format
  changes come with compatibility tests and a migration.
- **The engine is deterministic.** Same inputs and time, same pose.
- **Bounded memory.** Caches, bitmap pools and undo history all have size limits and an owner who
  clears them.
- **No hidden services.** Dependencies are passed in where objects are created; there is no global
  registry.
- **Offline first.** Nothing needs an account or network connection.

## Decisions

Important choices are recorded as architecture decision records:

- [Project architecture](decisions/project-architecture.md)
- [Design system boundary](decisions/design-system-boundary.md)
- [Library-only sharing](decisions/library-only-sharing.md)
- [Standard parameter limits](decisions/parameter-limits.md)

See also [module boundaries](modules.md), [code guidelines](../code-guidelines.md),
[development setup](../development.md) and [UI contributions](../ui-contributions.md).
