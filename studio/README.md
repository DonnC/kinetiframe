# studio

The editor's logic: its state and intents, the managers that change them, drawing tools, frame
rendering and caches, the camera, studio playback, video export, and the library's storage and
bundled content. It depends on `core`, `engine`, `data`, `media` and `actors`, and holds no
screens. The editor's screens and `StudioViewModel` still live in `app` and move to
`presentation.studio` as the [restructuring tracker](../docs/restructuring.md) describes.

| Package | Contents |
| --- | --- |
| `studio` | `StudioState`, `StudioIntent`, `StudioEffect`, their reducers, `StudioStore` and `StudioSession`, which creates the managers |
| `studio.domain` | Managers for strokes and undo, layers, frames, cel holds, selection, text, the view, backgrounds, scene effects and playback, and Actor Studio's managers in `domain.actorstudio` |
| `studio.drawing` | Brushes, stroke geometry, ruler constraints and fills |
| `studio.render` | The frame compositor and the frame cache |
| `studio.camera` | Camera framing, recipes and poster frames |
| `studio.media` | Studio audio and video playback over `media`, and voice styles for audio clips (`VoiceStyles`) |
| `studio.reference` | The frame reference index and `FrameStructureListener` |
| `export.data`, `export.domain`, `export.model` | Video export: `ProjectExporter` and the `VideoExporter` interface the export screen uses, the video and audio encoders, and export presets |
| `library.data`, `library.domain`, `library.model`, `library.bundled` | Library storage; the library screen's catalog (`LibraryCatalog`), pack drafts for the Pack Creator, library drawings read as actor artwork, and moving items between the library and an open project; the pack, palette and organisation documents and pack frame tags; and the bundled demo, starter kit and sample actors |
| `project.data` | The `.kframe` archiver, which bundles a project with the actors it uses |
| `project.domain` | The project gallery's storage (`ProjectCatalog`), opening and saving an editor's project (`ProjectOpener`, `ProjectSaver`), and project sharing, importing and saving to the library (`ProjectTransfers`) |

Tests that read the app's assets from disk, need Robolectric, or drive `StudioViewModel` stay in
`app`.

```sh
./gradlew :studio:testDebugUnitTest
```

Licensed under [Apache-2.0](../LICENSE).
