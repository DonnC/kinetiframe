# Changelog

Notable changes for people who use KinetiFrame are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/) once the first release is tagged.

## Unreleased

### Added

- *Tavi presents · natural hands*, a second sample beside the original. Tavi's arms and hands are
  drawn rather than pasted: his sleeves bend at the elbow, his hands come out of the cuffs and
  turn with his forearms, and his arms move in arcs. Existing
  installs get it on their next launch.
- Four scene backgrounds: Bedroom, Library, Bus stop bench and Old village.
- Drawn actors: characters drawn with curves on their bones instead of pictures, so arms, legs
  and tails bend smoothly in any pose. They work for any kind of thing, not only people.
  *Tavi · Drawn* ships beside the cutout Tavi.
- Your own colours. Tap + in the colour picker to save the current colour to My colours, make
  palettes of your own, and long-press a colour to remove it. The picker also shows your recent
  colours and takes hex codes. The current colour sits on the tool rail, one tap away.
- Layer lock, and an eye on every layer. Tap a layer's opacity to set it with a slider.
- Export video from the editor's menu. It saves your work first.
- Autosave: projects save as you work and when you leave the editor. Turn it off in Settings.
- Collections: long-press a project and drop it on another to group them, such as the episodes of
  a series, or drop it on a collection to add it.
- New projects show a live preview of the frame rate you pick, and canvas shapes as picture cards.
- Backgrounds can come from your Library as well as your files.

### Changed

- On a tablet the timeline shows frames you can read: bigger thumbnails shaped like the canvas,
  with the asset track lanes and the audio strip widened to match, so a clip still sits over the
  frames it covers. Phones keep the compact strip.
- On a tablet the asset tracks open under the canvas, between the tools and layers, so the tool
  panel keeps its full height and nothing in it is cut off.
- The tablet top bar names the frame Reference and Camera buttons, like the rest of the bar.
- A finished export plays in its preview, inside the app, instead of opening another app. Tap the
  video to stop it.
- While playing, the frame strip follows the frame on screen.
- Export shows what you are about to make: a preview shaped like the video, the project's length,
  frame rate and output size, and video sizes as cards with their shape and dimensions. Progress
  shows over the preview, and a finished video can be watched from the preview, saved to Gallery
  or shared. On a tablet the preview and the details sit side by side.
- A project's frame rate is set when it is created and no longer changes in the studio, so timing
  you have drawn stays as you drew it.
- The tablet studio keeps the canvas clear: the frame reference and camera buttons, Background,
  Frame image and Focus are on the top bar, the menu no longer repeats what the bar shows, the
  tool panel fits the screen without scrolling (one colour row instead of the full picker), and
  the panels step aside while the animation plays.
- Library on a tablet lays palettes and audio out as grids of cards, and its search box stops
  growing instead of stretching across the screen.
- New Pack on a tablet shows the form and the size comparison side by side, with compact S, M and
  L options instead of three huge empty squares.
- Camera effects are picked from tiles that each draw and play their move (push in, pan, shake,
  fade, whip pan…), with icons made for KinetiFrame, instead of a list of names.
- On a tablet in landscape the drawing studio is a workspace: tools, colour and the chosen tool's
  settings in a panel on the left, layers on the right, a labelled top bar with Frames, Onion
  skin, Grid, Safe frame, Tracks, Save and Export out in the open, and asset tracks
  docked under the canvas instead of covering it. The panels stay put, so the canvas never jumps.
- Tablets and foldables get layouts of their own instead of a stretched phone layout: a
  navigation rail instead of the bottom bar, project, actor and pack grids with as many columns
  as fit, layers beside the canvas instead of over it, the drawing bar's buttons kept together,
  and Frames tiles shaped like the canvas. New Project, Settings and Export keep a readable width
  instead of stretching across the screen. Layouts follow the window's width, so split screen and
  landscape phones get them too.

- A new identity: a bouncing-ball logo that stays clear at small sizes, the Nunito font across
  the app and in the wordmark, and one brand colour, carmine, on graphite and paper. Every
  interface colour is a tone of that carmine, the greys are neutral so they don't tint your
  artwork, and onion skins are carmine before and blue after, which people with red-green
  colour blindness can tell apart.

- KinetiFrame follows your phone's orientation. The studios no longer turn the screen to
  landscape by default, and browsing is no longer locked to portrait. Settings can still lock the
  studio to one orientation.
- A held drawing, such as a background, stays held on frames you add inside it or after its end.
- Actors' views are now called angles.
- Asset tracks open as a half-height sheet in portrait that you can drag taller or close, and the
  canvas sizes for a new project are a scrollable sheet.
- The bottom bar's tabs stay in the same place on every screen, and Home's New button says what
  it makes. Home, Library and Settings share one app bar.
- Library's filters are toggles: with none chosen you see everything, so there's no All chip.
- Library has a tab for each kind of item, including Images and Colours, and its search sits in the
  app bar. The editor's toolbar spreads its tools evenly and adds Copy, Paste and Reset view, which
  brings back the zoom and tilt. Safe frame moved to the menu.
- Every card in Home and Library looks the same: the picture on top and the name underneath, easy
  to read on any artwork.
- Library's Colours tab is a page of its own. My colours are the quick colours under the colour
  wheel, as large tiles with a + to add one. Palettes start from a New palette card, where you
  name one and add as many colours as it needs (up to 128). Each palette opens with a chevron to
  show its colours, add or remove them, rename it or delete it; a palette that comes with the app
  can be copied to edit. Tap a colour to copy its hex.
- The Library picker opens at least 45% of the screen tall in portrait.
- New Project suggests a name and starts with the sizes animators post to most: TikTok (9:16, also
  Reels and Shorts), Instagram (4:5), YouTube (16:9) and Facebook (1:1). New projects start
  vertical. Custom and every other size, including Instagram's 3:4 grid, are under Custom and more.
- The frame-rate preview bounces a ball straight up and down with faint copies of its last few
  frames, so the gaps show how smooth each rate is.
- Home's pack cards show a pack's drawing on light paper, as Library does, so dark line art
  reads in dark mode.
- Library cards drop wording that said nothing ("Empty root", "Personal Library"), and image
  previews lose their "Static preview" label.

### Fixed

- Export's preview showed blank paper for a project whose thumbnail was written after its last
  save, and a short animation's length read "0:00"; lengths under a minute now show seconds.
- Tool menus beside the dock are wider, never taller than the screen (a long one scrolls instead
  of running off the bottom), and the brush picker is a compact outlined button.
- The camera button shows all the time, below the frame reference button. The two sat on top of
  each other, so the camera only appeared during playback.
- A frame you deleted right after drawing on it no longer comes back in the next frame you add,
  and deleting or pasting frames while drawing no longer overwrites a neighbouring frame.
- One colour for every tool: the colour you pick is the one the pen, shapes, fill and text use,
  and the studio opens with the last colour you picked instead of black.
- Paste puts a drawing exactly where it was copied from, so it carries to the next frame
  without moving. It used to land a little down and to the right.
- The editor's frame-rate chips wrap onto a second line instead of hiding 24 and 30.
- Colours: the editor's colour picker chooses palettes from a dropdown instead of a row of chips
  that cut the last palette off, My colours
  can't be deleted, and long-pressing a colour shows its hex with Remove, as in Library, rather
  than removing it at once. A screen reader says a saved colour's hex once, not twice.
- Making a palette no longer grows the sheet under your finger when you add its first colour.
- New Project says why Create is greyed out until the project has a name, and Custom size starts
  from the size you had chosen, so its fields and label agree.
- Home opens on Recent when the app starts, every project card shows its length as time and
  frames ("0:02 · 48 frames"), and a new collection suggests a name not already in use.
- Library's project sheet and the canvas size list stay clear of the navigation bar.
- Frames can be reordered: in Frames, long-press a frame and drag it to its new place, or select
  one and move it earlier or later. The timeline's frame
  menu has Duplicate, and the Frames actions say what they do ("Paste after selection").
- Wording: New Project's square card is "Facebook post", as on Export; the sample actor is
  "Tavi · Four angles" on new installs; and a new performance gets a number ("New performance 2")
  instead of repeating a name the actor already uses.
- Fill fills with one tap; Undo takes it back. There is no confirm step any more.
- Onion skin has a switch on the drawing bar that shows whether it is on, and shows every visible
  layer, with "Active layer only" in its panel.
- The tool rail is tall enough for all its tools, so Fill is no longer cut off.
- Holding a drawing is picked on the timeline: choose Held drawing, then tap frames before or
  after to set the range. A hold can now reach back as well as forward, and there's no frame
  number to type.
- Each layer has a menu with Rename, Duplicate, Merge down and Delete. Tapping a layer selects it
  instead of starting a rename, + adds a drawing layer straight away, and the layers panel is only
  as tall as its layers, so it covers less of the canvas.
- Shapes has Line, Rectangle and Ellipse beside the construction guides, and starts on Line. The
  brush name on its card is readable in dark mode.
- Export has a back arrow, and a finished export can be watched before you save or share it.
- Timeline and Frames thumbnails show the whole canvas on white paper, in the canvas's shape. A
  tall canvas used to be cropped to a square on a dark tile, so frames looked empty.
- The frame-rate preview plays at the exact rate chosen. It used to run slow, so 24 fps looked
  like about 20.
- Dragging a project onto another to start a collection could be swallowed by the card's tap.

- Leaving the editor only asks to save when something changed. Saving shows a short notice
  instead of a dialog you have to dismiss.
- Adding an actor performance that makes the scene longer tells you, with Undo.
- Back during a lasso move keeps the move.
- Picking a colour from black no longer gives black: the wheel starts at full brightness.
- A fresh install no longer opens on a "Clean the trial library?" prompt.
- The canvas no longer jumps when playback starts.

## 0.1.0 - not yet tagged

The first release. The maintainer sets the date when `v0.1.0` is tagged. It needs Android 9
(API 28) or later.

### Added

- A per-app language setting, so KinetiFrame can use a different language from the rest of the
  phone. Debug builds also offer pseudo-locales for checking layouts with longer and right-to-left
  text.
- All of the app's text, including error messages, slider values, camera presets and the default
  library collection's name, now comes from string resources and can be translated.

### Fixed

- Sharing an exported video.
- Error messages now say what failed in words instead of showing internal error text, except for
  a technical detail added after a few rare storage failures.

### Removed

- The Mira and Kuda sample actors and their cutout packs. Tavi is the bundled actor. Copies
  already installed stay in your library.
- The engine sandbox app and the `-PengineOnly` build. Engine work runs the engine's tests and is
  checked in the app itself.
- The placeholder Marketplace tab and every "From Marketplace" entry. Sharing works through the
  library, `.kframe` project files and `.kpack` stamp packs.
- The internet permission. KinetiFrame works fully offline.
- Unused libraries, including Google Play Services Location, CameraX, Retrofit and Moshi, which
  makes the app smaller and free of proprietary dependencies. OkHttp still comes in with the Coil
  image loader, but the app makes no network requests.

### Changed

- The app ID, module namespaces and source packages are now `com.donnclab.kinetiframe`. Saved
  files keep their original type identifiers, so existing projects still open.
- The whole project, including the Android app and the bundled sample artwork, is now licensed
  under Apache-2.0.
- Starter actors are stored separately from sample scenes, and retired sample projects were
  removed.
- Debug builds install as `com.donnclab.kinetiframe.dev`, and every successful build of
  `develop` publishes a development APK.

### Known limits

- KinetiFrame is still in development. Projects saved by this version will keep opening in later
  versions, as the [file formats](docs/file-formats.md) promise, but screens and controls will
  change.
- Some screens have known layout and labelling problems, listed in the
  [restructuring tracker](docs/restructuring.md#ux-fixes).
- Sharing a large project can take minutes with no progress shown.

### For contributors

- New contributor documentation: README, contributing guide, glossary, roadmap, governance,
  architecture overview with diagrams, file formats and code guidelines.
- The code of conduct now follows the Contributor Covenant 2.1.
- `./gradlew verify` runs every check CI runs. Android lint is part of it.
- Kotlin code is formatted with Spotless and ktlint (IntelliJ IDEA style, 120-character lines).
  `./gradlew spotlessApply` fixes formatting, and `verify` checks it.
- Parameter limits now follow common Kotlin conventions (six for functions, seven for
  constructors), which shrank the legacy baseline from 352 to 59 entries.
- `-PengineOnly=true` replaces `-PpublicOnly=true` for the small engine and sandbox build. The old
  name still works.
- Dependabot, CODEOWNERS, an `.editorconfig`, and issue templates for artwork contributions.
- The shared design system now holds the Compose theme, visual tokens and dock controls.
- Working notes go in a Git-ignored `notes/` folder.
- `app` is organised by feature (`studio`, `actors`, `library`, `project`, `audio`, `video`,
  `export` and others) instead of by layer, and the editor's main types are named `Studio*`. See
  the [project architecture](docs/architecture/decisions/project-architecture.md) decision.
- ViewModels take their IO dispatcher as a parameter, so tests control all the work they start.
- The code is split into nine modules: `core`, `engine`, `data`, `media`, `actors`, `studio`,
  `presentation`, `designsystem` and `app`. Only `presentation`, `designsystem` and `app` use
  Compose, and screens reach storage only through their ViewModels. `checkArchitecture` enforces
  the layout. See the [feature modules](docs/architecture/decisions/feature-modules.md) decision.
- Every module except `app` is in `modules/`, and bundled assets have no version folders. The
  artwork source folders and pack-building scripts are removed; the shipped assets are the only
  copy.
- Every visual size and almost every colour comes from the design system's tokens.
- Tests live in the module whose code they cover. The device tests use the `v2` Compose test rule.
- The build targets Java 17 and uses Kotlin 2.4, AGP 9.5, Gradle 9.9 and JUnit 6.

No release history has been reconstructed from earlier development commits.
