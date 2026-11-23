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

### Changed

- A new identity: a bouncing-ball logo that stays clear at small sizes, the Nunito font across
  the app and in the wordmark, and one brand colour, carmine, on graphite and paper. Every
  interface colour is a tone of that carmine, the greys are neutral so they don't tint your
  artwork, and onion skins are carmine before and blue after, which people with red-green
  colour blindness can tell apart.

- KinetiFrame follows your phone's orientation. The studios no longer turn the screen to
  landscape by default, and browsing is no longer locked to portrait. Settings can still lock the
  studio to one orientation.

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
