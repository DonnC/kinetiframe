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

Work collects on one milestone branch, `milestone/code-limits`, with a commit per chunk and one
pull request into `develop` when the milestone is done. #75 finished the previous milestone
(per-app language, pseudo-locales and theme tokens).

**Parameter limits.** 50 of the 51 functions over the limit are fixed, mostly by grouping
parameters that travel together into small data classes. Each function unpacks them under the
old names, so behaviour is unchanged:

- The studio's managers take a `StudioRuntime`, the shared `UndoStacks` and a hooks class each.
  The library's exports take a `LibraryPlacement`.
- Home takes `HomeStates` and `HomeIntents`; its tabs take callback classes like the Packs tab's
  `PacksTabCallbacks`.
- The timeline clip takes `ClipGestures`; the clip, asset and background menus and the editor top
  bar take action classes.
- Actor Studio's preset sheet takes a `PresetPicker` and `PresetAudition`, and `StudioViewModel`
  takes the session's `StudioDirectories`.
- The canvas drawing layer takes a `CanvasSheet`, `OnionSkinLayers`, `CanvasContent`,
  `StrokePreview` and `SelectionOutline`, and the input layer an `InputViewport`, `InputTool`,
  `InputTargets` and `InputCallbacks`.
- Blur and smudge pass a reused, mutable `ManipulationDab`, so applying a dab still allocates
  nothing; particle stamps take the `TouchPoint` they already built.

Some parameters turned out to be unused and are gone: three in the selection box, the stroke
drawing's source bitmap and strength, the selection maths' gesture scale and the artwork skin's
softness. The one function left is `CubicStrokeSampler`'s recursive `subdivide`. It passes the
curve as twelve plain values on purpose, so subdividing allocates nothing, and it stays in the
baseline.

**Labels in the app's own components.** The inline text check now also counts the label of
`ContextIconAction` and `ContextSliderButton`, and any `label`, `title`, `subtitle`,
`placeholder` or `supportingText` argument. It skips private preview sample builders such as
`previewSample()`. The 48 labels it found are string resources: the timeline's clip and add-asset
menus, the background menu, the colour controls, the frame studio's toolbar and the blur size
slider. New clips are named from resources too ("Voice-over", "Imported video", "Video sound").
The English text is unchanged, so device tests that find these labels by text still match.

**Pseudo-locale check.** Home, Library, Settings and the editor were checked on a Galaxy A56 in
`en-XA` (long text) and `ar-XB` (right to left). Right to left mirrors cleanly. Fixed from the
pass:

- The bottom bar's labels and the frame thumbnails' descriptions were English; frames saved to
  the library were named "Frame N" in English. All three come from resources, and the check now
  counts an `Image`'s positional description.
- A project card's two badges overlapped with long text; they share one row now.
- Home's browse chips cut long words mid-letter; they end in an ellipsis now.
- Menu tiles cut labels to one line ("Actor perfor…"); they take two lines now.
- A long settings title ran into its switch; the row has a gap now.

## Next

1. **UI separation.** Remove the 38 Compose imports in domain, data, render, rig, math and model
   code, and move `StudioState`'s two onion skin colours to the theme.
2. **Right-to-left editor controls.** In `ar-XB` the editor's playback buttons and frame strip
   mirror, so "next frame" sits on the left and time runs right to left. Material keeps media
   controls left to right. Decide whether the timeline should too, and flip the undo and redo
   icons, which swap places but don't mirror.

## Later

Tracks that run alongside the module work, a screen or feature at a time:

- **Strings to resources.** The inline text check is at zero and the pseudo-locale pass is done.
  Left: the default collection's label (see Discoveries) and `require` messages in `core`'s
  documents, such as FPS and stage limits, that only invalid files reach.
- **Theme tokens.** Sizes are done. The last two inline colours go with UI separation.
- **UI separation.** Under Next. Screens already reach storage only through their ViewModels
  (step 8d).
- **Parameter limits.** Done, except `CubicStrokeSampler.subdivide`, kept on purpose (see Now).
- **Actor preset types.** The app's `ActorMotionPreset`, `RotationChannel` and `AppliedActorPreset`
  still mirror `core` types, with bridges and parity tests between them, and `ActorView` duplicates
  `core`'s `PresetAuthoredView`. Merge each pair and delete the parity tests.
- **Screen tests left in `app`.** Some tests of `presentation` code still live in `app` and use
  its public API: host tests that need Robolectric or the app's assets, the `studio` package's
  tests that drive `StudioViewModel`, and most device tests in the `studio` and
  `actors.ui.rigging` packages. Move them once `presentation` can run Robolectric tests and read
  the fixtures.
- **Preview fixtures.** The shared fixtures moved to `presentation.preview`. Split them by feature
  as the screens that use them settle.
- **Dependency updates.** Work through the Dependabot pull requests (#1 to #5) one family at a
  time, with a full check and a device check after each. The Compose BOM is already at the
  version #3 proposes.
- **Compose deprecations.** BOM 2026.09.00 deprecates `createComposeRule` (47 device tests), in
  favour of a `v2` rule that queues coroutines instead of running them at once, so tests may need
  explicit waits. The tab rows and tooltips are migrated; check the tabs on a device, since the new
  rows draw a different indicator, and check scrubbing the audio ribbon.
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

- In the brush panel, the brush name ("Pen") is white text on the white preview card.
- The export screen has no back button.
- In portrait, Actor Studio's hint banner runs into the gold asset button.
- The asset tracks panel covers the whole stage and says only "No clips yet".
- "Save before leaving?" appears even right after saving with nothing changed. In that dialog,
  Discard looks like the safe buttons although it throws work away.
- A successful save opens a blocking dialog that needs OK. A short message would do.
- Unlabelled top bar icons: the document and eye icons, and the round gold button on the right.
- The tool dock's grip doesn't read as draggable.
- Frame thumbnails are dark grey while the canvas is white, so strokes are barely visible in them.
- With one frame, the frame strip floats in the middle of a mostly empty bar.
- Undo doesn't remove a frame added with the frame strip's +.
- The camera effects button is hidden under the frame reference button: both sit in the canvas's
  top-right corner, and the reference button is drawn on top. The camera button only appears in
  focus mode.

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
- Sharing the 34 MB demo project took about three and a half minutes on an A73, with no sign
  that anything was happening after the menu closed. Show progress, and look at why the
  archive is slow.

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
| #36 | Linted every module in `verify`, each with its own baseline |
| #37 | Built the app's services in one container (step 8c) |
| #38 | Ran Home and the library through their ViewModels (step 8d, part 1) |
| #39 | Moved the library's documents to `library.model` (step 8d, part 2) |
| #40 | Let ViewModels use the logic modules' repositories (step 8d, part 3) |
| #41 | Gave the library picker its own ViewModel (step 8d, part 4) |
| #42 | Ran the export screen through its ViewModel and fixed sharing a video (step 8d, part 5) |
| #43 | Ran the Pack Creator through its ViewModel (step 8d, part 6) |
| #44 | Ran Actor Studio's artwork picker through its ViewModel (step 8d, part 7) |
| #45 | Moved the project gallery's storage into `ProjectCatalog` (step 8d, part 8) |
| #46 | Saved editor projects through `EditorStorageViewModel` (editor storage, part 1) |
| #47 | Moved the editor's library actions into `LibraryTransfers` (editor storage, part 2) |
| #48 | Gave the editor's actor rendering and timeline narrow actor services (editor storage, part 3) |
| #49 | Removed the editor's and Actor Studio's store bags (editor storage, part 4) |
| #50 | Split the library catalog into a studio reader and a card mapper |
| #51 | Moved the app's preferences into `studio`; no screen imports a `data` package (step 8d done) |
| #52 | Moved what the studio screens shared with `app` into `presentation` (step 9, part 1) |
| #53 | Moved the editor, library, export and project screens into `presentation` (step 9, part 2) |
| #54 | Moved Home and Settings into `presentation`, ending the module work (step 9 done) |
| #55 | Made `checkArchitecture` describe the nine-module layout |
| #56 | Used one Compose version (BOM 2026.09.00) in every module and fixed the newer lint checks |
| #58 | Moved the shell screens' text into string resources (strings, part 1) |
| #59 | Gave ViewModels `UiText` instead of English sentences (strings, part 2) |
| #60 | Moved the camera effect screens' text into string resources (strings, part 3) |
| #61 | Moved the timeline dialogs' text into string resources (strings, part 4) |
| #62 | Moved the rest of the timeline's text into string resources (strings, part 5) |
| #63 | Moved the Actor Studio screen's text into string resources (strings, part 6) |
| #64 | Moved Actor Studio's rigging tools' text into string resources (strings, part 7) |
| #65 | Moved Actor Studio's sketch tools' text into string resources (strings, part 8) |
| #66 | Moved the library's text into string resources (strings, part 9) |
| #67 | Moved the rest of the actor screens' text into string resources (strings, part 10) |
| #68 | Moved the editor's text into string resources (strings, parts 11 and 12) |
| #69 | Named model values and built-in camera presets from string resources |
| #70 | Returned typed validation failures from `core` and worded them in the UI (typed errors, part 1) |
| #71 | Reported studio errors as typed failures (typed errors, part 2) |
| #72 | Gave storage failures a typed reason and worded the editor's messages from resources (typed errors, part 3) |
| #73 | Typed fixable problems and tightened the inline text check (typed errors, part 4) |
| #74 | Worded media failures, slider values and audio tags from resources (typed errors, part 5) |
| #75 | Added per-app language and pseudo-locales, and moved all inline sizes and all but two inline colours into tokens |

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
  Locally, `./gradlew -p buildSrc test` right after a full build can fail to resolve the Android
  plugin's accessors (`Unresolved reference 'android'`). Stop the daemons and run it on its own.
  When the IDE's Gradle process keeps `buildSrc/build` locked, run the checker tests on a copy of
  `buildSrc` (with `gradle/libs.versions.toml` beside it) instead. AAPT2 can also stop starting
  from Gradle ("Daemon startup failed") even though `aapt2 daemon` runs by hand; CI's
  manual run covers `app` when that happens. A compile that runs while sources are broken can
  leave a bad entry in the local build cache: `designsystem` once came back with an empty
  `.kotlin_module`, so `presentation` couldn't see its top-level declarations (`BrandTeal`,
  `KinetiFrameTheme`). Rebuild with `--no-build-cache` when a module's top-level names go missing.
- **Lint version checks.** Lint's "newer version available" checks are disabled, because their
  findings change with every upstream release. Dependabot reports updates instead.
- **Checker tests.** CI runs `./gradlew -p buildSrc test`, but only for `main` during the
  restructuring, and `verify` can't run them. Three tests still named `actors` as an app feature
  after it became a module. Run them by hand whenever the checker or the package layout changes.
- **Implicit Compose versions.** Navigation 3 raised Compose in `app` above the BOM that the
  other modules compiled against, so `presentation`'s lint skipped four Compose checks. A
  transitive dependency can do this again: compare `./gradlew :app:dependencies` with
  `:presentation:dependencies` when bumping a library that depends on Compose.
- **The default collection is saved in English.** `LibraryOrganization` stores "General
  Essentials" as the default collection, and the store and browser compare against that text. It
  is saved data, so translating it needs a migration. Keep it as the saved key and show a
  translated label wherever a collection name is displayed.
- **Unused legacy migration.** `LegacyActorMigration.performance` has no production callers; only
  tests use it. Delete it with its tests once old actor files no longer need converting.
- **Library build defaults.** Library modules relied on default SDK levels until the convention
  plugins set them (#17).

## Debt counts

From `.architecture/` during step 9. Each count may only go down.

| Debt | Count | Files |
| --- | --- | --- |
| Compose imports outside UI code | 38 | 25 |
| UI imports of `data` packages | 0 | — |
| Functions over the parameter limit | 1 | — |
| Inline text values | 0 | — |
| Inline colours | 2 | 1 |
| Inline `.dp` sizes | 0 | — |
| Files over 500 lines | 0 | — |
