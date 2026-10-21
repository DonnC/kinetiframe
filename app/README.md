# app

The KinetiFrame Android app. It holds the entry point, navigation, every product screen
including the studio, platform adapters for drawing, audio and video, repositories, the Room
database and the bundled sample assets. It depends on all the other production modules.

Much of the product code still lives here while shared pieces are moved into their target modules.
Some packages inside `app` are named `engine`, `data` and `domain`; they are folders in this
module, not the modules of the same name. The
[architecture overview](../docs/architecture/README.md#where-to-find-things-in-app) has a map of
where each feature lives.

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
