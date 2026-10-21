# Directional body footwear and resting-hand sources

Generated bitmap atlases used by the sample character builders. These sources are retained
because current builders consume them before newer source overrides.

- footwear-atlas.png: Tavi, Mira and Kuda rows; front, back, profile and three-quarter columns.
- hands-atlas.png: matching resting hands with wrist overlap.
- Per-character full-body/v5/sources/body-atlas.png: blank-face heads and separate torsos.

Registration measures neck/wrist positions from authored pixels. Facial anchors are in source
space. Profile far-side limbs retain independent graph parts even when overlapped at rest.
Do not mistake a 2D view change for a simulated 3D turn.

Use the current joined builders documented in the [source guide](../../../../README.md),
not a historical version-specific output path. Validate all views and motion extremes in native
renders and on device. Keep transparency, palettes and outlines consistent with newer overrides.
