# Directional gesture-hand sources

Generated transparent sprite atlases for palm, point and fist gestures. The v5 body, footwear
and resting-hand inputs remain separate builder dependencies.

Each atlas has four view columns (front, back, profile, three-quarter) and three character
rows (Tavi, Mira, Kuda). Source wrist geometry is registered to the resting hand. Opposite-side
sprites use mirrored artwork where appropriate; profile parts remain independently posable.

These are authored 2D cutouts, not simulated three-dimensional hands. Preserve the source
alpha and wrist registration. Do not hide articulation defects with renderer-only corrections.

Use the current joined builders in the [source guide](../../../../README.md). Inspect neck,
wrist and silhouette behavior through the full gesture on native renders and a device.
Existing sample render sheets are references, not a signoff for every extreme pose.
