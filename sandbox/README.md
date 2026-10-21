# sandbox

A small development app for working on the engine without the full product. Drag procedural
FABRIK joints, record up to 300 target samples, replay and reset them. A toggle switches between a
bone-only view and a mesh preview that uses the same skin deformation code as the app.

It uses only `core` and `engine`, has no production artwork and needs no network permission.
Recording here is a debugging aid, not KinetiFrame's saved performance format.

```sh
./gradlew -PengineOnly=true :sandbox:installDebug
./gradlew -PengineOnly=true :sandbox:testDebugUnitTest
./gradlew -PengineOnly=true :sandbox:connectedDebugAndroidTest   # needs a device
```

`-PengineOnly=true` builds only `core`, `engine` and this app, which is much faster than the full
build. Controls and canvas have Compose previews.

See [development setup](../docs/development.md). Licensed under [Apache-2.0](../LICENSE).
