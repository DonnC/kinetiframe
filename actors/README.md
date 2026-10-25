# actors

Rigged characters: their storage, rig math, posing, performances and rendering. It depends on
`core`, `engine` and `data`, and holds no screens. The actor library screens live in
`presentation.actors`, and Actor Studio, a studio mode, lives in `app` under
`studio.ui.actorstudio` and `studio.domain.actorstudio`. Both call into this module.

| Package | Contents |
| --- | --- |
| `data` | Repositories for actors, performances, placements, presets and bundles, and the artwork store |
| `domain` | Placement timing and trimming, performance evaluation and duration, recorded motion curves, root motion, legacy migration |
| `render` | `ActorFrameRenderer`, which draws placements for the editor, export and thumbnails, plus artwork rendering, hit testing and thumbnails |
| `rig` | Rig math (`rig.math`), the rig model (`rig.model`), motion presets and their bridges to `core` (`rig.presets`), and texture extraction (`rig.texture`) |

Bundled actors, presets and preset icons live in the app's assets and are read through a `Context`
at run time. Tests that read those files from disk, or that need Robolectric, stay in `app`.

```sh
./gradlew :actors:testDebugUnitTest
```

Licensed under [Apache-2.0](../LICENSE).
