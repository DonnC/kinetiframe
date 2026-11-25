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
into `develop` when the milestone is done. The module restructuring is finished (#54 to #88), and
#89 prepared version 0.1.0: the release checklist passed up to signing. #91 cleaned up the
repository. #90 merged `develop` into `main` before #91, so `main` doesn't have the cleanup yet.
#92 shipped the natural-hands copy of `Tavi presents`.

**Natural actors.** `milestone/natural-actors` reworks actors so they look drawn and are easy to
pose. The plan and the decisions behind it are in `notes/actor-redesign.md` (local). So far:

- Four new scene backgrounds, and the Gemini copies removed from `docs/images`.
- Drawn actors. `SceneGraph.figure` holds curves instead of image cutouts. One base serves every
  kind of thing, with nothing human-specific in the model or renderer:
  - parts: rigid drawings on one bone, with variants;
  - tubes: inked tubes along bone chains, which can taper, bend at every joint and have cuffs;
  - surfaces: drawings that bend with the nearest bones.
- Drawn figures render through the same plan as cutouts, in the editor, export, thumbnails and
  Actor Studio.
- *Tavi · Drawn* is bundled beside the cutout Tavi, with front view only for now.
- Animate mode shows handles only at the tips of chains, and dragging one bends the chain through
  IK. Every joint stays editable in Setup.
- Skeleton templates are one registry (`SkeletonTemplate`). Two legs, Four legs and Start from
  scratch are entries in it. A new kind of actor, such as a bird or a car, is one class that
  lists its bones, and gets posing, keys, recording, presets and saving from the shared base.
  Saved template IDs no longer need to be in a fixed list.
- The studio's tool dock fits the tools a mode shows, instead of keeping a fixed height with
  empty space where tools were hidden.
- Animate mode's hint describes posing pose to pose. Recording a take stays as an option, and its
  hint shows only while a take runs.
- Actor Studio, checked on a Galaxy A73:
  - The mode switch reads "Setup | Animate" in words.
  - The duplicate bone buttons in the top bar and dock are gone.
  - The actor is framed large in every mode, in the area the panels leave clear.
  - The selected part's tools sit in the part rail instead of floating over the actor.
  - The part chips use theme colours and count drawn pieces as artwork.
  - Drawn actors don't show the continuous-drawing toggle, and get a Setup hint about their build.
  - Animate offers a drawn part's variants, such as Tavi's hand poses, from a labelled chip on
    the stage's edge.
- The app follows the phone's orientation instead of forcing landscape in the studios and
  portrait elsewhere. In portrait, Actor Studio stacks the part tools above the chip rail, keeps
  the hint clear of the image button, and wraps the timeline controls. Checked on a Galaxy A56.

Next, in a follow-up milestone: setup sliders for proportions, then side views by mirroring.

**Brand.** `milestone/brand` replaces the teal and amber identity, which read as generic, with
one drawn from the animator's desk. The reasons are in [the brand](assets.md#the-brand).

- The mark is a bouncing ball in three frames on one diagonal: round, stretched as it falls, and
  squashed on the ground, in carmine and graphite on a paper tile. It reads at 16 px and as a
  themed icon.
- The interface and the wordmark use Nunito, bundled as five static weights under the OFL. The
  wordmark is Nunito ExtraBold with carmine balls for the i dots.
- One brand colour: the logo's carmine is the primary, and every other interface colour is a
  tone of it from its Material HCT palette, over the logo's graphite and paper neutrals. The
  greys are neutral, so the interface doesn't tint the artwork.
- Every brand image is a vector generated from one geometry: the launcher, themed and splash
  icons, the in-app mark and wordmark (`BrandArt`), and the SVGs and board in `docs/brand`. The
  generator is in `tools/brand`. The raster brand PNGs and the `tools/branding` scripts are gone.
- The default onion skins are carmine before and blue after, the paper convention. That blue is
  the only colour outside the brand, and only on the canvas.
- Checked on a Galaxy A73 in the dark theme: splash, Home and the editor. The light theme and the
  launcher icon on a home screen aren't checked on a phone yet.

**Studio essentials.** `milestone/studio-essentials` fixes what the first UX review round found.
The six-persona panel that ran it is in [tools/ux-review](../tools/ux-review/README.md).

- Trust: leaving only asks to save when something changed since the last save, and a
  successful save is a short notice instead of a dialog. Adding an actor performance that
  lengthens the scene says so and offers Undo. Back during a lasso move applies the move. A fresh
  install no longer opens on the trial-library cleanup. The canvas no longer jumps when playback
  starts.
- Colour: the current colour is a dot on the tool rail, the wheel starts at full brightness
  from a dark colour, and the picker has recent colours, My colours with a + to save the current
  colour, and hex entry. Palettes you make can be added to, trimmed by long-press and deleted;
  starter palettes stay read-only.
- Layers: every layer shows its eye and lock, and its opacity opens a slider. A held drawing
  continues onto frames added inside it or after its end, so a background held to the end stays
  held as the scene grows.
- Words and layout: actor views are called angles. Asset tracks open as a half-height sheet in
  portrait that can be dragged taller or closed, and New Project's canvas sizes are a scrollable
  sheet. The bottom bar has three equal tabs on every screen, with a labelled New button above it
  on Home. Home, Library and Settings share the same app bar, with no back arrow on Settings, and
  Library's filter chips toggle instead of starting with All. Export video is in the editor's
  menu and saves first.

**UX round 2.** `milestone/ux-round-2` acts on the maintainer's own review after Studio essentials.

- Autosave is on by default: gallery projects save a few seconds after editing pauses, when the
  app goes to the background and when leaving the editor, so leaving no longer asks. A Settings
  switch turns it off. Save is in the editor's menu, and Pack Creator and Actor Studio keep their
  explicit saves.
- The drawing bar spreads Home, Undo, Redo, Copy, Paste, Safe frame and Asset tracks evenly, with
  Focus and Save in the menu. Speed is now Frame rate, with the same picker as New Project and a
  note that drawings stay while speed changes. Background asks Library or files.
- Root screens share one app bar with no title under it. Library's bar holds the logo, its search
  and refresh; Library has tabs per kind (Projects, Actors, Packs, Images, Audio, Colours), and
  Colours reuses the studio's colour panel, so the palette icon is gone.
- New Project picks the canvas from picture cards (Landscape, Portrait, Square, Custom; Local is
  now Custom) with every platform size in More sizes, and the frame rate from chips with a live
  bouncing-ball preview. New Pack shows each size against the largest. Add flows have a proper app
  bar.
- Collections show as cards. Long-press a project and drop it on another to start a named
  collection, or on a collection to add it; Move to collection stays in the project menu.
- The Library picker used by the editor and asset tracks is a draggable bottom sheet, with toggle
  chips like Library's.

**Tavi presents with natural hands.** `milestone/tavi-hands` ships a copy of `Tavi presents`
beside the unchanged original:

- Arms and hands are drawn with curves in every frame instead of pasted cutouts, matching the
  [model sheet](images/tavi-model-sheet.png): one continuous sleeve from shoulder to wrist with
  a soft elbow, a fold when it bends, and a cuff whose mouth overlaps the wrist. The relaxed,
  point, fist and palm-up hands are chunky mittens with the thumb on the silhouette.
- The arms are in the hands layer (renamed "arms + hands"), above the desk, so a hand on the desk
  or the chest stays attached to its arm. Where an arm leaves the shirt, its outline joins the
  shirt's, so the shoulder reads as one silhouette.
- Hands turn with the forearm, and a spring makes them trail a turning arm and settle. Arms ease
  in joint angles, so hands travel on arcs. The palm, point and fist poses are re-authored.
- Torso, head, desk, eye and mouth artwork is the original. The sample is 32 MB.

## Next

1. **First release.** A maintainer merges `develop` into `main`, signs a release build and tags
   `v0.1.0`, following steps 5 to 8 of the [release checklist](releases.md#release-checklist).
   The font licences must be added first (see Discoveries).
2. **UX fixes.** Decide whether the bundled Tavi actor gets the same hand treatment (see UX
   fixes), then the Actor Studio and editor items.
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

**Tavi's hands**

- Tavi's expressions animate well, but the hands move like glued-on cutouts. They don't bend like
  the hand poses on the [model sheet](images/tavi-model-sheet.png),
  so the actor looks stiff. Don't change the originals: make copies, improve them and ship both,
  so the app itself shows the difference.
- `Tavi presents` was drawn layer by layer without the rig. Each hand was an upright stamp with
  its own darker cuff, pasted over a rounded sleeve end and barely turned with the forearm, and
  the forearms sat behind the torso and desk while the hands sat in front. The natural-hands copy
  draws the arms and hands instead (see Now).
- The rigged Tavi actor has the same problem: large, detailed hand cutouts with an outlined wrist,
  drawn on top of three rigid arm cutouts. A copy of the actor needs drawn, bendable sleeves and
  mitten hands like the sample's, which means the actor renderer must draw curves, not only
  place images.

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
- The asset tracks sheet says only "No clips yet" when empty, with no hint of what to add.
- In "Save before leaving?", Discard looks like the safe buttons although it throws work away.
- "Menu labels" only labels context menus. The top bar, frame menu and selection menu stay
  icon-only, and long labels such as "Edit asset tracks" need a layout that fits seven icons.
- Library's Colours tab shows the eyedropper (pick from screen), which only makes sense in the
  studio. Library picker cards show empty selection boxes that do nothing.
- The frame strip doesn't follow the playhead during playback.
- Frames could not be reordered by dragging in the frame manager in a scripted test; check by
  hand. There is no one-tap duplicate and no hold count per frame in the strip.
- Fill and lasso need a confirm tap every time. Consider applying on the next action.
- "Playback FPS" changes the project's frame rate, not only the preview.
- In portrait the canvas fills about a quarter of the screen, and Focus hides the controls
  without enlarging it.
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

- Project thumbnails are transparent PNGs. A black drawing shows as an empty dark card. Draw
  thumbnails on the stage's white paper.
- A one-frame project shows a duration of "0:00".
- Card badges (fps, size) have no scrim over light artwork.
- Actor cards have no visible surface, and the "4 angles" chip floats above them and repeats "All
  four views" in the title.
- Actor angles are listed alphabetically; Front should come first.
- Home and Library both list projects, packs and actors, so it isn't clear what Library is for.
- Pack items do nothing when tapped, and their names look like file names ("ls-eyes-neutral").
- Putting a character in a scene takes four or five steps through Asset tracks.
- Leaving Actor Studio returns to the Projects tab, not the Actors tab it was opened from.
- On a fresh install the Library's Audio tab says "No matching items. Try another filter or
  search." with no filter or search applied.
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
| #89 | Set version 0.1.0, moved the changelog under it and recorded the release checks |
| #91 | Removed the art sources and pack builders, shipped unversioned assets with Tavi only, and moved the modules into `modules/` |

The combined result of #13 to #18 was checked on a device: opening and playing an existing
project, drawing, saving and reopening a new one, and applying a motion preset in Actor Studio.

## Discoveries

- The 17 fonts in `app/src/main/assets/fonts` are Google Fonts, most under the SIL Open Font
  License, which `THIRD_PARTY_NOTICES.md` doesn't list. Add each font's license before a release.
- Mira was the only bundled actor drawn as one continuous piece with skinned bones. With her gone,
  no shipped actor exercises that path; cover it with a test fixture when the actor work resumes.
- Coil 2.7 brings OkHttp into the release build for loading images over the network, which the
  app never does. Coil 3 without its network module would drop it. The resolved release libraries
  are Apache-2.0, apart from the Lucide icon set, whose artwork is ISC. Keep its notice with
  releases.
- [File formats](file-formats.md) promises that unknown JSON fields are ignored on read, but
  the actor, performance and preset repositories use `ignoreUnknownKeys = false`. An older app
  therefore rejects a revision that has a new field, such as a drawn actor's `figure`, instead of
  loading what it understands. Either relax the readers or correct the promise.

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
