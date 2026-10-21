# File formats

KinetiFrame saves your work as ordinary files: JSON for documents and PNG for drawings. Two
archive formats are used when work leaves the app: `.kframe` for projects and `.kpack` for stamp
packs. Both are standard ZIP files you can open with any archive tool.

This page describes the layout as it is today. It is not yet a frozen, versioned specification.
Field-level details live in the Kotlin models named below, and compatibility tests protect them.

## Project folder

While you work, a project is a folder in the app's private storage:

```
projects/<project-id>/
  manifest.json        schema version, name, stage size, frame rate, frame count, timestamps (ProjectManifest)
  document.json        layers, frames, actor placements, audio, video, camera moves (ProjectSaveDocument)
  frames/              PNG images for drawn frames and layers
  performances/        performance documents used by this project's placements
  project-assets/      imported audio, images and video
  vault/               project-scoped library items
```

The project ID is a random UUID. Projects are not zipped while you edit, because re-compressing
hundreds of PNGs on every save would be slow.

## `.kframe` project archive

A `.kframe` is created when you share or back up a project
(`app/.../data/project/KframeArchiver.kt`). It contains:

```
manifest.json          the project manifest
document.json          the project document
snapshot.json          manifest and document together; import checks they match
archive-paths.json     the original project folder path and any relocated assets
frames/ ...            everything from the project folder
performances/ ...
project-assets/ ...
portable-assets/       assets that lived outside the project folder, given new names
dependencies/actors/<actor-id>/ ...   every actor the project places, with all revisions
```

On import, KinetiFrame:

1. Unpacks into a temporary folder and rejects any entry whose path would escape it.
2. Reads and validates `manifest.json`, then the full `document.json`.
3. Rewrites stored absolute paths using `archive-paths.json`, so assets resolve on the new device.
4. Refuses the import if a project with the same ID already exists, rather than overwriting it.

## `.kpack` stamp pack

A `.kpack` holds a pack's drawings and a `meta.json` index at the root of the ZIP
(`app/.../data/library/PackArchiver.kt`):

```
meta.json
frame_000.png
frame_001.png
...
```

`meta.json` (`PackManifest`):

```json
{
  "pack_name": "Mouth shapes",
  "author": "",
  "total_items": 2,
  "canvas_dimension": 512,
  "items": [
    { "file": "frame_000.png", "index": 0, "name": "Rest", "desc": "" },
    { "file": "frame_001.png", "index": 1, "name": "Open", "desc": "" }
  ]
}
```

Rules checked on import:

- `pack_name` has 1 to 60 characters.
- `total_items` equals the number of `items`, and there is at least one.
- `canvas_dimension` is positive.
- Each `file` is a plain file name, with no path separators, and file names are unique.
- Item names have at most 40 characters and descriptions at most 200.

An item can also carry an optional `registration` that tells Actor Studio how to attach it to a
body part. Imported packs always receive a new local ID, so two people's packs with the same name
never collide.

## Actors and performances

Actors are stored as folders with a manifest and immutable revisions. Each revision holds the
rig (`SceneGraph`) and artwork. Performances are JSON documents tied to an actor revision. Their
models are in `core/.../actor` and `app/.../data/actor/ActorDocuments.kt`.

Some older serialized type names still start with `com.kinetiframe.` (the project's previous
package name). They are part of the file format and must not be renamed.

## Compatibility promise

- Files saved by an older version of KinetiFrame keep opening in newer versions. When a format
  changes, the change includes a migration and a test that loads the old format.
- Unknown JSON fields are ignored on read, so a newer file opened by an older app loses the new
  data but still loads where possible.
- Imports are validated before anything is written to your library or projects.

If you are building a tool that reads or writes these files, please open an issue. It will help
us decide what to freeze in a formal specification.
