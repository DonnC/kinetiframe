# Development setup

This guide gets you from a fresh clone to a running app and a passing test run.

## Requirements

- **JDK 17.** In Android Studio, set it under *Settings > Build Tools > Gradle > Gradle JDK*.
  On the command line, point `JAVA_HOME` at it.
- **Android SDK platform 37.** Android Studio installs it when you sync. With the command-line
  tools, run `sdkmanager "platforms;android-37"` and accept the licenses.
- **A device or emulator running Android 9 (API 28) or newer.** A real phone or tablet is best for
  drawing, stylus, audio and export work.

You don't need to install Gradle. The checked-in wrapper downloads the right version. The first
build also downloads dependencies, so it needs a network connection.

If Gradle can't find the SDK, create an untracked `local.properties` file in the repository root:

```properties
sdk.dir=/path/to/Android/sdk
```

## Open in Android Studio

Open the repository root folder (not `app/`) and let Gradle sync. Pick the `app` run
configuration and press Run.

Debug builds install as `com.donnclab.kinetiframe.dev`, so they sit next to a stable install
without touching its projects. Data is not shared between the two.

## Build from the command line

The examples use the Unix wrapper. On Windows, use `gradlew.bat`.

```sh
./gradlew :app:assembleDebug        # build the APK
./gradlew :app:installDebug         # build and install on a connected device
```

The APK is written to `app/build/outputs/apk/debug/`.

## Check your change

One command runs everything CI runs on your machine:

```sh
./gradlew verify
```

That is the formatting check, the architecture check, every module's host tests and Android lint.
Run `./gradlew spotlessApply` first to fix formatting automatically. Device tests need a
connected phone or emulator and run separately:

```sh
./gradlew :app:connectedDebugAndroidTest
./gradlew :designsystem:connectedDebugAndroidTest
```

To run a single test class, add `--tests` to its module's test task:

```sh
./gradlew :engine:testDebugUnitTest --tests '*SkinDeformationTest*'
```

### The architecture check

`./gradlew checkArchitecture` reads every Kotlin file and fails if:

- a file is longer than 500 lines
- a function or lambda has more than 6 parameters, or a constructor more than 7
  (data classes are exempt; in composables only required parameters count)
- a module imports something it shouldn't (see [module boundaries](architecture/modules.md))

Older code that already breaks these rules is listed in `.architecture/legacy-baseline.properties`.
Never add entries to that file. If you shorten a function on the list, run
`./gradlew reportArchitectureDebt` and copy the smaller result from
`build/architecture/candidate-baseline.properties`.

The checker lives in `buildSrc` and has its own tests: `./gradlew -p buildSrc test`.

### Lint

Android lint runs as part of `verify`. Existing warnings are recorded in `app/lint-baseline.xml`,
so only new problems fail the build. Open `app/build/reports/lint-results-debug.html` to read the
report.

## Engine work

If you are working on rigging, IK or deformation, run the engine's tests on their own:

```sh
./gradlew :core:test :engine:testDebugUnitTest
```

Check the result in the app's Actor Studio before opening a pull request.

## Tests and test data

- `core` and engine math tests use JUnit 5 and need no Android, network or disk.
- `app` tests use JUnit 4/5 and Robolectric. Some read the bundled sample assets directly, so
  changing a sample asset re-runs them.
- Storage and migration tests build temporary projects instead of relying on old archives.
- Render tests write images to `.artifacts/` for review. That folder is ignored by Git, and
  running tests never changes the shipped sample projects.

## Working notes

Keep personal notes, plans and scratch files in `notes/` at the repository root. Git ignores it.

## Troubleshooting

| Problem | Try |
| --- | --- |
| "Unsupported class file major version" or toolchain errors | Check `./gradlew --version` reports JDK 17 |
| SDK location not found | Set `ANDROID_HOME` or create `local.properties` with `sdk.dir` |
| Dependency downloads fail | The first build needs access to Google Maven, Maven Central and the Gradle distribution server |
| App won't install over an older debug build | Debug signing keys can differ between machines and CI. Export your projects, uninstall, then install again |
| Architecture check fails on a file you didn't touch | Pull the latest `main`; the baseline may have changed |

When reporting a build problem, include the full Gradle output, but remove any paths or tokens
you don't want to share.

## Build tooling

- `buildSrc` holds the architecture checker, not app code. It runs the Kotlin parser in a separate
  process so it doesn't interfere with the Kotlin Gradle plugin.
- Dependency versions live in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). Don't
  upgrade them in an unrelated pull request. Dependabot opens grouped update pull requests monthly.
- `.editorconfig` holds shared formatting settings that Android Studio picks up automatically.
  Spotless runs ktlint with the same settings: the IntelliJ IDEA code style, 120-character lines
  and no wildcard imports. Composable functions may start with an uppercase letter.
