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

Work collects on one milestone branch per milestone, with a commit per chunk and one pull request
into `develop` when the milestone is done. The module restructuring is finished: #54 ended the
module work, and #75 to #87 finished strings, tokens, code limits, UI separation, the preset
types, tests and dependency updates. `milestone/finish` (#88) closed the remaining tracks.

**Release preparation.** `milestone/release-0.1.0` runs the
[release checklist](releases.md#release-checklist) up to signing:

- The app's `versionName` is `0.1.0`, and the changelog's entries sit under 0.1.0 with known
  limits. The maintainer dates it when tagging.
- CI passed on `develop`, `./gradlew verify` passes locally, and the release build assembles.
- No keys, keystores, tokens or passwords appear anywhere in the history.
- The development APK installs on an A56 with Android 17. `Tavi presents` opens and plays, using
  about 330 MB, and its camera effects open. A new project draws, saves, reopens and exports a
  video. Tavi opens in Actor Studio and its Performance Stage. Audio, import, updating over an
  older install and older sample archives were not checked on the phone; the format tests cover
  older files.

Signing, the tag and the GitHub release stay with the maintainer.

The notes below are from `milestone/finish`.

**Compose test rule.** All 46 device test files use the `v2` `createComposeRule`, which queues
coroutines on a `StandardTestDispatcher` until the test idles. Run on an A56 with Android 16:

- Compose's test library brought an Espresso too old for Android 16, so every Compose module's
  device tests now depend on Espresso 3.7.0 through the Compose convention plugin.
- `DockToolButtonTest` advances the test clock past the button's double-tap wait, which the `v2`
  rule doesn't do on its own.
- `presentation` fails the same 13 tests that failed before the restructuring, and nothing else.
  `app` and `designsystem` pass.

**Preview fixtures.** Every user of `PreviewCatalog` is in the library, so it moved to
`presentation.library.preview`.

**Strings.** `DEFAULT_COLLECTION` names the default library collection in `studio`. It stays the
saved English key, and the library's collection menu shows it through a string resource. The
`require` checks in `core`'s documents keep English messages on purpose: only corrupt files break
them, and the screens show a translated failure first, with that text only as a technical detail.

**Changelog.** The changelog's Unreleased section now covers the user-visible changes and the
contributor changes since #8, ready for the first release.

## Next

1. **First release.** A maintainer merges `develop` into `main`, signs a release build and tags
   `v0.1.0`, following steps 5 to 8 of the [release checklist](releases.md#release-checklist).
2. **UX fixes**, starting with the improved copy of `Tavi presents` (see UX fixes).
3. **Old device test failures.** Fix or delete the 13 failing `presentation` device tests as the
   code they cover changes (see Later).

## Later

Tracks that run alongside the module work, a screen or feature at a time:

- **Strings to resources.** Done (see Now). `core`'s `require` messages stay English on purpose.
- **Theme tokens.** Done.
- **UI separation.** Done: only `presentation`, `designsystem` and `app` use Compose, and screens
  reach storage only through their ViewModels (step 8d). Checked on a phone.
- **Parameter limits.** Done, except `CubicStrokeSampler.subdivide`, kept on purpose: its recursion
  passes the curve as twelve plain floats so that subdividing allocates nothing.
- **Actor preset types.** Done (#78).
- **Screen tests left in `app`.** Done (#80). Three remain in `app` on purpose:
  `ViewPresetCollectionTest`, `NaturalActorMotionTest` and `BundledDemoLibraryInstallerTest` check
  the app's shipped assets.
- **Logic tests left in `app`.** Done (#86). The tests that read the app's shipped assets stay in
  `app`, since those assets are what they check.
- **Preview fixtures.** Done (see Now).
- **Dependency updates.** Done (#79, #87). Dependabot keeps proposing updates; take them a group
  at a time, with a full check and a device check after each.
- **Compose deprecations.** Done: the device tests use the `v2` rule and run on Android 16 (see
  Now), and the tab rows and tooltips are migrated. Left: check the tabs on a phone, since the new
  rows draw a different indicator, and check scrubbing the audio ribbon.
- **Device tests.** 13 `presentation` device tests fail and failed before the restructuring began.
  They check labels and controls that have since changed. Fix or delete each one when the code it
  covers changes:
  - `ActorPartTreeTest.boundAndUnboundPartsShowDistinctArtworkStatus`
  - `ActorSketchBindingTest.sharedDockSelectsJointModeWithoutDrawing`
  - `CanvasFirstStudioTest.speedIsOnlyExpandedOnRequest`
  - `CompactColorControlsTest.disabledFieldCannotEdit`
  - `DrawingCanvasTest.stationaryTapCommitsOneContact` and `testDrawingStrokeEmitsPoints`
  - `HierarchyBrowserTest.collectionBrowserHasNoPackCategoriesAndSelectsCollection`
  - `PenContactRasterTest.stationary_contacts_leave_the_same_visible_disk_at_any_sample_count`
  - `PerformanceAuthoringControlsTest.diamondSelectsExactSavedTime` and
    `performanceToolbarExposesActorUndo`
  - `PuppetWorkspaceTest.studioBarReturnsToRasterAndTogglesHierarchy`
  - `SelectionDockTest.skewIsDiscoverableWithoutPersistentControls`
  - `TrackClipContextMenuTest.firstTapShowsIconPaletteAndDuplicateDispatchesOnce`
- **First release.** The changelog is ready (see Now). A maintainer tags `v0.1.0` from `main`.

## UX fixes

Found during device checks on a Galaxy A56. None of them comes from the restructuring. They are
fixed after the module work, unless one blocks testing.

**Tavi presents** (first task of the next phase, on its own branch)

- Tavi's expressions animate well, but the hands move like glued-on cutouts. They don't bend like
  the hand poses on the [model sheet](art-direction/loose-signal/characters/tavi/references/model-sheet.png),
  so the actor looks stiff. Either rework the actor's rigging and hand cutouts, or animate the
  hands frame by frame.
- Don't change the bundled `Tavi presents`. Make a copy, improve the copy and ship both, so the
  app itself shows the difference.

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
- The "Push in" camera preset's End framing shows black beyond the bottom of the stage in
  `Tavi presents`.

**Home and actors**

- A fresh install opens on "Clean the trial library?", although there is nothing to clean yet.

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
| #76 | Brought 50 of 51 functions under the parameter limit, counted component labels as inline text, and fixed the pseudo-locale pass |
| #77 | Took Compose out of `actors`, `studio` and `media`, which now use `core` geometry (UI separation) |
| #78 | Merged the app's actor preset types into `core`'s and deleted the bridges and parity tests |
| #79 | Updated Kotlin, AGP, Gradle, JUnit and the other dependencies, and moved to Java 17 bytecode |
| #80 | Moved the screen and ViewModel tests from `app` into `presentation` |
| #86 | Parsed sources without `KotlinCoreEnvironment` and moved the logic tests from `app` into their modules |
| #87 | Updated Gradle 9.9.1, AGP 9.5.1 and the rest, and let IDE imports create the Compose Preview task |
| #88 | Moved the device tests to the `v2` Compose rule and Android 16, moved the preview fixtures and translated the default collection's name |

The combined result of #13 to #18 was checked on a device: opening and playing an existing
project, drawing, saving and reopening a new one, and applying a motion preset in Actor Studio.

## Discoveries

- Coil 2.7 brings OkHttp into the release build for loading images over the network, which the
  app never does. Coil 3 without its network module would drop it. The resolved release libraries
  are Apache-2.0, apart from the Lucide icon set, whose artwork is ISC. Keep its notice with
  releases.

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
- **Duplicate actor preset types.** The app's preset types mirrored `core` types. #14 and #21
  moved the two saved fields that used them to the `core` types, which was all the document move
  needed. The actor presets milestone merged the rest.
- **Right-to-left editor controls.** In `ar-XB` the editor's playback buttons and frame strip
  mirror, so "next frame" sits on the left and time runs right to left, while Material keeps media
  controls left to right. The undo and redo icons swap places but don't mirror. This is left to
  another contributor and isn't part of this plan.
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
  is saved data, so translating it needs a migration. It is now `DEFAULT_COLLECTION`, kept as the
  saved key, and the collection menu shows a translated label. The dialogs that rename a collection
  still show the saved name, since that is the text being edited.
- **CI's development APKs can't update each other.** CI signs each run's debug APK with a new
  debug key, so installing one over another (or over a local build) fails with a signature
  mismatch, and the only way through is uninstalling, which wipes the app's data. Sign debug
  builds with a shared development keystore, kept as a CI secret, so `adb install -r` works.
- **Unused legacy migration.** `LegacyActorMigration.performance` has no production callers; only
  tests use it. Delete it with its tests once old actor files no longer need converting.
- **The checker's compiler API.** Kotlin 2.4 marks `KotlinCoreEnvironment` for rework, and the
  architecture checker used it to parse sources. It now sets up only IntelliJ's core PSI
  environment (see Now). That environment and its headless setup still come from
  `kotlin-compiler-embeddable`, so a Kotlin update can still break them; the checker's tests catch
  that. Kotlin also still warns that `kotlin-compiler-embeddable` is on the build classpath, since
  `buildSrc` compiles the checker against it. Moving the checker into its own build, included with
  `includeBuild`, would remove the warning.
- **Library build defaults.** Library modules relied on default SDK levels until the convention
  plugins set them (#17).

## Debt counts

From `.architecture/` during step 9. Each count may only go down.

| Debt | Count | Files |
| --- | --- | --- |
| Compose imports outside UI code | 0 | — |
| UI imports of `data` packages | 0 | — |
| Functions over the parameter limit | 1 | — |
| Inline text values | 0 | — |
| Inline colours | 0 | — |
| Inline `.dp` sizes | 0 | — |
| Files over 500 lines | 0 | — |
