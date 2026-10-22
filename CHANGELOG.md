# Changelog

Notable changes for people who use KinetiFrame are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/) once the first release is tagged.

## Unreleased

### Removed

- The placeholder Marketplace tab and every "From Marketplace" entry. Sharing works through the
  library, `.kframe` project files and `.kpack` stamp packs.
- The internet permission. KinetiFrame works fully offline.
- Unused libraries, including Google Play Services Location, CameraX, Retrofit, OkHttp and Moshi,
  which makes the app smaller and free of proprietary dependencies.

### Changed

- The app ID, module namespaces and source packages are now `com.donnclab.kinetiframe`. Saved
  files keep their original type identifiers, so existing projects still open.
- The whole project, including the Android app and the bundled sample artwork, is now licensed
  under Apache-2.0.
- Starter actors are stored separately from sample scenes, and retired sample projects were
  removed.
- Debug builds install as `com.donnclab.kinetiframe.dev`, and every successful build of
  `develop` publishes a development APK.

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

No release history has been reconstructed from earlier development commits.
