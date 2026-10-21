# Sample character and scene sources

The app's sample catalog uses Tavi, Mira and Kuda with separately authored front, back,
profile and three-quarter views. Character cutouts v6 are in curated/v4; shared expressions,
eyes, mouths and studio items v3 are in curated/v3.

## Builder inputs

- characters/{tavi,mira,kuda}/full-body/v7: current repaired head/torso sources.
- full-body/v6: directional hand alternatives.
- Earlier full-body/body/face/hand sources: limbs, shoes and facial variants still consumed
  by street_actor.py, cast_views.py and directional_bindings.py before current overrides.
- characters/_shared/rig-sources: source atlases and generation provenance.
- clips/cast-all-views/v4: native actor render references.
- clips/tavi-presents: presentation sample source.

Version numbers alone do not determine obsolescence. Preserve a source until its current
builder consumers have been updated and verified. See [the asset guide](../../assets.md).

## Rebuild

From the repository root, run `python tools/loose_signal/build_starter_actors.py` to build the
actor-only source ZIP. This offline builder requires Python and Pillow. Then run
`tools/loose_signal/build_cast_view_packs.ps1 -Joined` and
`tools/loose_signal/build_curated_packs.ps1 -Joined` in PowerShell to rebuild character packs.
These commands replace shipping files; review the diff and run actor/catalog/render tests.
Shared expression, eye, mouth and studio packs have one canonical copy in curated/v3.
The retired scene builders and historical project archives are not part of this workflow.

Artwork sources include generated bitmaps and code-drawn components. Keep their provenance
metadata. Project-owned sources are licensed under the repository's Apache-2.0 license.
Check assembled native renders and motion extremes after repairs; a complete file list or
passing numerical test is not evidence of seamless joints in every pose.
