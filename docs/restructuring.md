# Restructuring tracker

What's done, what's in progress and what comes next in the restructuring of KinetiFrame. The plan
comes from the [feature modules](architecture/decisions/feature-modules.md) decision.

Keep this page current:

- A pull request that starts or finishes a task moves it to **Now** or **Done** here.
- Anything discovered along the way goes under **Discoveries**, or into a list below if it needs
  work.
- Debt counts come from the `.architecture/*-baseline.properties` files. Update the table when a
  pull request lowers them.

## Now

**Step 8c: one place for the app's services, in review.** `AppContainer` in `app` creates the
database, the document store, every repository and the preference store once, and
`AppViewModelFactory` builds the ViewModels from it. Screens get their ViewModel with
`appViewModel()` instead of building stores, and routes receive the shared stores instead of a
storage folder. The [dependency wiring](architecture/decisions/dependency-wiring.md) decision
explains the approach and when to revisit Hilt.

## Next

The remaining steps, in order, following the
[UI in presentation](architecture/decisions/ui-in-presentation.md) decision. Nine modules in all:
no `export`, `library`, `home` or `settings` modules.

1. **Studio screens.** The editor's screens, `EditorScreen`, `StudioViewModel`, and the library,
   export and project screens move to `presentation.studio`, `presentation.library` and so on.
2. **Shell screens.** Home, settings and project creation move to `presentation`. Navigation and
   wiring stay in `app`, which then holds nothing else.

## Later

Tracks that run alongside the module work, a screen or feature at a time:

- **Strings to resources.** 298 inline text values in 95 files, plus messages assigned to state
  that the check doesn't count. Then right-to-left layouts, per-app language and a pseudo-locale
  check.
- **Theme tokens.** Move 64 inline colours and 806 inline `.dp` sizes into `designsystem` tokens.
- **UI separation.** Remove the 38 Compose imports in domain, data, render, rig, math and model
  code, and the 114 places where UI code imports a `data` package. Screens should reach storage
  through their ViewModel, as in strict MVI.
- **Parameter limits.** 53 functions still take more than the allowed number of parameters.
- **Actor preset types.** The app's `ActorMotionPreset`, `RotationChannel` and `AppliedActorPreset`
  still mirror `core` types, with bridges and parity tests between them, and `ActorView` duplicates
  `core`'s `PresetAuthoredView`. Merge each pair and delete the parity tests.
- **Typed errors in `core`.** `ActorGraphValidation` and `PresetValidationDiagnostics` turn typed
  validation issues into English sentences inside `core`. Return the typed issue and let the UI
  map it to string resources.
- **Preview fixtures.** Give each module its own `preview` package and split up the shared one
  in `app`.
- **Dependency updates.** Work through the Dependabot pull requests (#1 to #5) one family at a
  time, with a full check and a device check after each. The Compose BOM (2024.09.00) is the
  furthest behind.
- **Device tests.** 13 device tests fail on `develop` and failed before the restructuring began.
  They check labels and controls that have since changed. Delete each one when the code it covers
  is replaced.
- **First release.** Tag `v0.1.0` with a changelog and release notes.

## UX fixes

Found during device checks on a Galaxy A56. None of them comes from the restructuring. They are
fixed after the module work, unless one blocks testing.

**Actor Studio**

- The part chips (Head, Eyes, Mouth…) are pale text on pale chips over a white band: unreadable.
- That band cuts across the tool dock and covers the bottom of the stage.
- The character is drawn small. In Pose mode the joint pins hide it completely.
- The floating part toolbar sits on top of the character.
- About nine unlabelled icons in the top bar. The bone icon appears twice, and a faint icon in the
  stage's top-left corner has no explanation.
- The tool dock clips the timeline label ("rame 1 · 0 keys").
- The timeline shows "0 keys" while a motion preset animates the actor.
- The preset sheet covers the stage, so you can't see the preview you're choosing.
- Selecting a preset makes the sheet jump to full height and moves the apply button.
- The apply button looks enabled before a preset is chosen.

**Editor**

- The asset menu cuts off "Actor perfor…" and "Import audio …".
- The asset tracks panel covers the whole stage and says only "No clips yet".
- "Save before leaving?" appears even right after saving with nothing changed. In that dialog,
  Discard looks like the safe buttons although it throws work away.
- A successful save opens a blocking dialog that needs OK. A short message would do.
- Unlabelled top bar icons: the document and eye icons, and the round gold button on the right.
- The tool dock's grip doesn't read as draggable.
- Frame thumbnails are dark grey while the canvas is white, so strokes are barely visible in them.
- With one frame, the frame strip floats in the middle of a mostly empty bar.

**Home and actors**

- Project thumbnails are transparent PNGs. A black drawing shows as an empty dark card. Draw
  thumbnails on the stage's white paper.
- A one-frame project shows a duration of "0:00".
- Card badges (fps, size) have no scrim over light artwork.
- Actor cards have no visible surface, the "4 views" chip floats above them and repeats "All four
  views" in the title, and the last card sits under the + button.
- Actor views are listed alphabetically; Front should come first.
- Leaving Actor Studio returns to the Projects tab, not the Actors tab it was opened from.
- On a fresh install the Library's Audio tab says "No matching items. Try another filter or
  search." with no filter or search applied.
- Settings is a bottom bar tab but shows a back arrow and a second header under the brand bar.

**System bars**

- The brand bar draws over the status bar on the New Project screen.
- The Create Project button touches the navigation bar.
- The actor details sheet draws its last row under the navigation bar.

## Done

| Pull request | What it did |
| --- | --- |
| #6 | Grouped `app` by feature and adopted the project architecture |
| #7 | Split six oversized files by responsibility |
| #8 | Split `StudioViewModel` and `EditorScreen`; every file is now under 500 lines |
| #9 | Counted inline colours, sizes and text in `checkArchitecture` |
| #10 | Accepted the feature modules decision and checked feature layering |
| #11 | Moved `FeatureStrings` into string resources |
| #12 | Removed Android and studio code from the project model |
| #13 | Replaced app type aliases with the `core` types |
| #14 | Saved actor motion presets as the `core` preset document |
| #15 | Moved the project document model into `core` (step 1) |
| #16 | Checked that Compose stays in UI code and UI reaches storage through ViewModels (step 2) |
| #17 | Shared module build settings through convention plugins (step 3) |
| #18 | Removed the engine sandbox app; CI runs one job |
| #19 | Started this tracker |
| #20 | Moved generic storage and crash reports into `data` (step 4a) |
| #21 | Moved the actor documents into `core` (step 4b) |
| #22 | Ran CI only for pull requests and pushes to `main` during the restructuring |
| #23 | Moved the Room database and project storage into `data` (step 4c) |
| #24 | Moved the shared app UI into `presentation` (step 4d) |
| #25 | Moved studio media code into `studio.media` and extracted the `media` module (step 5) |
| #28 | Moved Actor Studio under the studio, and the scene camera and render cache into `engine` (step 6, part 1) |
| #29 | Extracted the `actors` module and ran the UI checks in every module (step 6, part 2) |
| #32 | Moved the actor and audio screens into `presentation`, and `StoreResult` into `core` (step 7) |
| #34 | Kept the studio's logic free of its screens (step 8a) |
| #35 | Extracted the `studio` module (step 8b) |

The combined result of #13 to #18 was checked on a device: opening and playing an existing
project, drawing, saving and reopening a new one, and applying a motion preset in Actor Studio.

## Discoveries

- **Project storage depends on actors.** One Room database holds the actor, performance, library,
  export and project tables. One file store, named `ActorFileStore` until 4a, serves every
  feature. `.kframe` archives check actor dependencies through `ActorRevision`, which pulls in
  the actor documents and the app's preset types. Step 4 is split into 4a to 4d because of this.
- **The `.kframe` archiver bundles actors.** `KframeArchiver` exports and imports a project
  together with the actors and performances it uses, through the actor repositories and
  `PerformanceCompatibility`. It stays in `app` until the actors module exists, then belongs above
  it.
- **Export renders through the studio.** `ProjectExporter` draws each frame with the studio's
  `ProjectFrameCompositor`, `FrameCacheManager` and `FontRegistry`, and the actor renderer. The
  compositor belongs below both `studio` and `export`, which is why export moves after the actors
  split instead of with media.
- **Unused imports go unnoticed.** ktlint 1.x no longer has its unused-imports rule, and nothing
  else in `verify` reports them. Moves leave unused imports behind, which also inflate the uidata
  count. Add a check, for example detekt's `UnusedImports`, or the compiler's warning as an error.
- **The UI rules read package names.** `uidata` counts UI code by its `ui` segment, and `compose`
  counts domain code by its `domain` segment. A move that leaves those segments hides debt, so
  moved screens go under a `ui` package and managers under `domain`.
- **Duplicate actor preset types.** The app's preset types mirror `core` types. #14 and #21 moved
  the two saved fields that used them to the `core` types, which was all the document move needed.
  Merging the rest is listed under Later.
- **`DurableFileTest` on Windows.** Its concurrent-writer test can fail with a file-lock error on
  Windows. It passes on rerun and in CI.
- **Local Windows builds.** The IDE's Java language server and Gradle daemons can hold files in
  `buildSrc/build` open and break local builds of the checker. CI on Linux is the reliable check.
- **Lint version checks.** Lint's "newer version available" checks are disabled, because their
  findings change with every upstream release. Dependabot reports updates instead.
- **Editor repositories.** `EditorRepositories` and the voice effect dialog still reach the
  container through the `Context` (`rememberEditorRepositories`), and Home, the library, export
  and pack creation still use stores directly for importing, sharing and archiving. Each goes
  through its ViewModel when its screen moves to `presentation`.
- **Library build defaults.** Library modules relied on default SDK levels until the convention
  plugins set them (#17).

## Debt counts

From `.architecture/` after step 8c. Each count may only go down.

| Debt | Count | Files |
| --- | --- | --- |
| References to a feature above | 24 | 15 |
| Compose imports outside UI code | 38 | 25 |
| UI imports of `data` packages | 114 | 44 |
| Functions over the parameter limit | 53 | — |
| Inline text values | 298 | 95 |
| Inline colours | 64 | 24 |
| Inline `.dp` sizes | 806 | 132 |
| Files over 500 lines | 0 | — |
