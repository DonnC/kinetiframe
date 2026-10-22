# app

The KinetiFrame Android app. It holds the entry point, navigation, every product screen
including the studio, platform adapters for drawing, audio and video, repositories, the Room
database and the bundled sample assets. It depends on all the other production modules.

Packages are grouped by feature (`studio`, `actors`, `library`, `project`, `audio`, `export` and
so on), with `ui`, `domain` and `data` subpackages inside a feature where it needs them. See the
[project architecture](../docs/architecture/decisions/project-architecture.md) decision and the
[map of where each feature lives](../docs/architecture/README.md#where-to-find-things-in-app).

## Things to know before changing code here

- Storage and media work runs off the main thread.
- Never decode images or allocate bitmaps inside a Compose draw phase.
- Long-running work must not hold on to an Activity.
- A gesture in progress is a draft. Only a finished gesture changes the saved document.
- Saved project, actor and performance formats must stay compatible. See
  [file formats](../docs/file-formats.md).

## Test

```sh
./gradlew :app:testDebugUnitTest             # host tests
./gradlew :app:connectedDebugAndroidTest     # device tests
./gradlew :app:assembleDebug
```

Licensed under [Apache-2.0](../LICENSE). Bundled content terms are in [NOTICE](../NOTICE).
