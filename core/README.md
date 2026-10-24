# core

Plain Kotlin with no Android dependencies: the data models, file codecs and rules that the rest of
KinetiFrame builds on. Because it runs on any JVM, its tests are fast, and tools outside the app
could read KinetiFrame files with it.

| Package | Contents |
| --- | --- |
| `actor.rigging` | Transforms, bones, scene graphs and graph validation |
| `actor.artwork` | Artwork registration, drawing and skin data, variants |
| `actor.motion` | Pose and contact records, motion channels, playback timing |
| `actor.presets` | Versioned motion preset documents and their codecs |
| `actor.validation` | Typed validation issues |
| `actor.document` | Saved actor, revision, performance, placement and library item documents |
| `document` | The project document: layers, frames, nodes, strokes, audio and video clips, actor instances |
| `document.camera` | Camera shots and the rules a saved list of shots follows |
| `geometry` | Points and 2D affine transforms |
| `contracts` | Small interfaces for replaceable services, with disabled defaults |
| `mvi` | `StateStore`, the state and effect plumbing behind every ViewModel |

## Things to know

- Codecs keep historical field names so older files still load, even where the Kotlin model
  groups fields differently.
- Decoding does not validate or repair. Validate before evaluating or saving, and turn typed
  issues into user-facing messages in the app.
- Models are immutable. `core` owns no files, bitmaps or Activities.
- A disabled service contract must report that it is unavailable, never pretend an operation
  succeeded.

When you add a field to a saved model, add tests that load files written before the change.

```sh
./gradlew :core:test
```

Licensed under [Apache-2.0](../LICENSE).
