# Artwork and sample assets

Project-owned bundled artwork, audio, brushes and branding are licensed under Apache-2.0,
as authorized by the maintainer. See [LICENSE](../LICENSE), [NOTICE](../NOTICE) and
[third-party notices](../THIRD_PARTY_NOTICES.md). A user's imported media is not covered by this grant.

## Source locations

| Location | Purpose |
| --- | --- |
| `app/src/main/assets/loose-signal` | Bundled characters, packs and example projects |
| `app/src/main/assets/actor` | Starter actor data and motion presets |
| `app/src/main/assets/library` | Library catalog data |
| `docs/art-direction/loose-signal` | Editable source artwork, manifests and render references |
| `docs/art-direction/backgrounds` | Background scene sources and manifest |
| `docs/art-direction/brand/approved` | Approved raster branding sources and export metadata |
| `tools/loose_signal` and `tools/branding` | Offline pack, scene and branding builders |

The current actor source is `app/src/main/assets/actor/starter-actors-v4.zip`: actor documents
and artwork only, with no project frames or saved performances. `Tavi presents` is the retained
frame-by-frame example. Retired scene archives and the educational demo are no longer bundled.

Do not delete an older source atlas based on its version number alone. Current artwork builders
still consume earlier pieces before applying newer overrides. Production manifests and scripts
identify those dependencies. Generated intermediate packs belong under `.artifacts`, not in a
historical project tree. Regeneration is separate from running the Android build.

## Contributing assets

Record authorship, original source, license, permitted redistribution and any generation process
with the source files. Keep editable sources when available. Do not copy artwork, voices, music,
screenshots or brushes from another application without compatible permission and attribution.

For actor kits, verify all authored views and motion extremes. Parts must overlap correctly at
neck, shoulders, waist, wrists and ankles, with matching pivots and registration. Check native
renders as well as thumbnails. Keep transparent cutouts free of background fringes.

For branding exports, preserve the approved source pixels and follow the checked-in metadata.
For audio, retain a lossless source, avoid clipping and check both phone speakers and headphones.
For generated content, record the tool/process honestly; do not invent an upstream artist or license.

Build changes must not overwrite shipping packs or sample projects without an explicit review
of the changed files and a render/import/export verification.
