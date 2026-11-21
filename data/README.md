# data

Storage building blocks shared across the app. It depends only on `core`.

| Package | Contents |
| --- | --- |
| `storage` | `DurableFile`, which writes to a temporary file, syncs it and then moves it into place, so a crash never leaves a half-written file. `DocumentFileStore`, the app-private folder for actors, projects and the library. `ContainedPaths` |
| `diagnostics` | `CrashReportStore`, one small private report of the last error |
| `db` | The Room database: a rebuildable index of projects, exports, actors, performances and library items. The files on disk are the source of truth |
| `project` | `ProjectRepository`, project manifests, saved project documents and frame drafts |

The other feature repositories still live in `app` and move here as the
[restructuring tracker](../docs/restructuring.md) describes. Keep storage
details behind the small contracts in `core`, don't leak database or SDK types into saved
documents, and add provider integrations only when they are implemented and tested. Nothing here
requires a network or cloud account.

Callers run file operations off the main thread.

```sh
./gradlew :data:testDebugUnitTest
```

Tests cover failed and concurrent writes. Licensed under [Apache-2.0](../LICENSE).
