# actors

Rigged characters: their storage, rig math, posing, performances and rendering. It depends on
`core`, `engine` and `data`, and holds no screens. The actor library screens live in
`presentation.actors`, and Actor Studio, a studio mode, lives in
`presentation.studio.actorstudio`, with its managers in `studio.domain.actorstudio`. Both call
into this module.

| Package | Contents |
| --- | --- |
| `data` | Repositories for actors, performances, placements, presets and bundles, and the artwork store |
| `domain` | Placement timing and trimming, performance evaluation and duration, recorded motion curves, root motion, legacy migration, and `PerformanceChoices`, the performances a timeline can place |
| `render` | `ActorFrameRenderer`, which draws placements for the editor, export and thumbnails; `ActorRendering`, which hands screens renderers without the actor stores; plus artwork rendering, hit testing and thumbnails |
| `rig` | Rig math (`rig.math`), the rig model (`rig.model`), motion preset playback and the preset catalog (`rig.presets`), and texture extraction (`rig.texture`) |

## Actors of any kind

Every actor uses the same base, whatever it is: a skeleton of any shape, and optionally a drawn
figure (`core/.../actor/figure`). Posing and IK, pose keys, recording, presets, performances,
rendering and saving work the same for every actor. Nothing in that code knows what an arm is.

A **skeleton template** is a ready-made starting skeleton for a common kind of actor. Two legs,
Four legs and Start from scratch are built in (`rig/model/SkeletonTemplate.kt`). To add one, such
as a bird or a car:

1. Implement `SkeletonTemplate`: a permanent `id` such as `bird-v1`, its bones as
   `TemplateBone`s, the `roles` that presets may rely on, and its root. Give a two-bone limb's
   lower bone a `chainRootId`, so dragging its tip bends the joint. Optionally return a default
   drawn figure from `figure`.
2. Add it to `SkeletonTemplates.all`.
3. Add a name for it to the app's string resources.

Saved actors keep their template's `id`. Never rename one. An ID the app doesn't know, such as
one saved by a newer version, opens as Start from scratch.

## Bundled content

Bundled actors, presets and preset icons live in the app's assets and are read through a `Context`
at run time. Tests that read those files from disk stay in `app`. Tests that need real bitmaps run
here on Robolectric.

```sh
./gradlew :actors:testDebugUnitTest
```

Licensed under [Apache-2.0](../../LICENSE).
