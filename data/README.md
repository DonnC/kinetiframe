# data

Storage building blocks shared across the app. Today this is `DurableFile`, which writes to a
temporary file, syncs it and then moves it into place, so a crash never leaves a half-written
project file. It depends only on `core`.

Most repositories still live in `app` (`app/.../data`) and will move here over time. Keep storage
details behind the small contracts in `core`, don't leak database or SDK types into saved
documents, and add provider integrations only when they are implemented and tested. Nothing here
requires a network or cloud account.

Callers run file operations off the main thread.

```sh
./gradlew :data:testDebugUnitTest
```

Tests cover failed and concurrent writes. Licensed under [Apache-2.0](../LICENSE).
