# app

The KinetiFrame Android app. It holds the entry point, navigation, the service container
(`AppContainer`) and ViewModel factory, the Home and Settings screens, and the bundled sample
assets. The other screens live in [`presentation`](../presentation/README.md). It depends on all
the other production modules.

Packages are grouped by feature (`home`, `settings`, `navigation`). Its tests still include
screen and studio tests that read the app's assets or need Robolectric. See the
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
