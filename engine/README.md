# engine

The animation math behind actors: inverse kinematics (FABRIK and analytic two-bone), world
transforms, pose and artwork editing, contact following, skin deformation, artwork registration,
pose and curve sampling, performance clocks, motion composition and motion presets. It depends
only on `core`.

This is a good place to contribute if you like geometry and graphics. You can try changes in the
[sandbox](../sandbox/README.md) app without building the full product.

## Things to know

- **No UI or storage here.** Math packages use no Android or Compose types and are checked for it.
  Convert Compose geometry at the UI edge.
- **Deterministic.** A sample depends only on its inputs and the time, never on the previous frame
  shown. Revisions passed in are immutable and already validated.
- **Bounded caches.** Every cache has an owner and an entry limit. Pose tracks keep 4 entries,
  rotation and root curves 128 and 32, and preset root curves 16. Callers decide sharing and
  clear them at lifecycle boundaries. Don't allocate per frame or per actor without a limit.
- **Presets compile lazily.** `AppliedPresetProgram` compiles rotation channels on first use, and
  duration or root-only queries don't touch them.

Algorithm changes need deterministic tests and a check in the product, not only a sandbox
screenshot.

```sh
./gradlew :engine:testDebugUnitTest
```

Licensed under [Apache-2.0](../LICENSE).
