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

**Step 4: data and presentation modules.**

- Move project storage (Room, project files, `.kframe` and `.kpack` archives) from `project.data`
  into the `data` module, along with crash reports from `diagnostics`.
- Move the shared app UI in `common` into `presentation`, alongside `BaseViewModel`.

## Next

The remaining steps of the feature modules decision, in order:

1. **Media and export modules.** Move `audio`, `video` and `media` into one `media` module and
   `export` into its own module.
2. **Actors split.** The actor domain (rig, posing, rendering, storage) becomes the `actors`
   module. Actor Studio screens that use `StudioState` move under `studio`. Today `actors` makes
   173 references into `studio`; this step removes them.
3. **Library, then studio.** Extract each module once it only references modules below it.
4. **Home and settings.** Extract them and move navigation into `app`, so `app` holds only the
   entry point and wiring.

## Later

Tracks that run alongside the module work, a screen or feature at a time:

- **Strings to resources.** 298 inline text values in 95 files, plus messages assigned to state
  that the check doesn't count. Then right-to-left layouts, per-app language and a pseudo-locale
  check.
- **Theme tokens.** Move 64 inline colours and 806 inline `.dp` sizes into `designsystem` tokens.
- **UI separation.** Remove the 38 Compose imports in domain, data, render, rig, math and model
  code, and the 206 places where UI code imports a `data` package. Screens should reach storage
  through their ViewModel, as in strict MVI.
- **Parameter limits.** 53 functions still take more than the allowed number of parameters.
- **Preview fixtures.** Give each module its own `preview` package and split up the shared one
  in `app`.
- **Actor preset types.** Finish replacing the app's preset types (`ActorMotionPreset`,
  `RotationChannel`, `ActorView`) with the `core` ones they bridge to, and delete the parity tests
  that compare them.
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

The combined result of #13 to #18 was checked on a device: opening and playing an existing
project, drawing, saving and reopening a new one, and applying a motion preset in Actor Studio.

## Discoveries

- **`DurableFileTest` on Windows.** Its concurrent-writer test can fail with a file-lock error on
  Windows. It passes on rerun and in CI.
- **Local Windows builds.** The IDE's Java language server and Gradle daemons can hold files in
  `buildSrc/build` open and break local builds of the checker. CI on Linux is the reliable check.
- **Lint version checks.** Lint's "newer version available" checks are disabled, because their
  findings change with every upstream release. Dependabot reports updates instead.
- **Editor repositories.** `EditorRepositories` creates the editor's stores inside a composable.
  They should be created in `app` and passed in, which the home-and-settings step allows.
- **Library build defaults.** Library modules relied on default SDK levels until the convention
  plugins set them (#17).

## Debt counts

From `.architecture/` on `develop` after #18. Each count may only go down.

| Debt | Count | Files |
| --- | --- | --- |
| References to a feature above | 238 | 81 |
| Compose imports outside UI code | 38 | 25 |
| UI imports of `data` packages | 206 | 60 |
| Functions over the parameter limit | 53 | — |
| Inline text values | 298 | 95 |
| Inline colours | 64 | 24 |
| Inline `.dp` sizes | 806 | 132 |
| Files over 500 lines | 0 | — |
