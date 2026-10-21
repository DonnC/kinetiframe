# Pack index

Current native packs ship from `app/src/main/assets/loose-signal/curated`.
Generated intermediate packs belong under `.artifacts`, outside the APK and source catalog.

| Category | Primary pack | Source and registered stamps |
| --- | --- | --- |
| Tavi four-view cutouts, including hands | ls-tavi-cutouts-v6 | curated/v4; characters/tavi/full-body |
| Mira four-view cutouts, including hands | ls-mira-cutouts-v6 | curated/v4; characters/mira/full-body |
| Kuda four-view cutouts, including hands | ls-kuda-cutouts-v6 | curated/v4; characters/kuda/full-body |
| Expression eyes + brows | ls-expressions-v3 | curated/v3 |
| Standalone eyes | ls-eyes-v3 | curated/v3 |
| Mouths | ls-mouths-v3 | curated/v3 |
| Studio furniture and props | ls-studio-items-v3 | curated/v3 |

The three v5 character kits in curated/v3 are migration comparison inputs only, not current
gallery packs. Shared packs have one canonical copy. See [source dependencies](../README.md).

Keep new guest IDs separate (`ls-<guest>-...`), and never overwrite a published pack ID for a breaking visual change.
