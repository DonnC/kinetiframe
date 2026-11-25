# Artwork and sample assets

Project-owned bundled artwork, audio, brushes and branding are licensed under Apache-2.0,
as authorized by the maintainer. See [LICENSE](../LICENSE), [NOTICE](../NOTICE) and
[third-party notices](../THIRD_PARTY_NOTICES.md). A user's imported media is not covered by this grant.

## Where the assets live

The app's bundled content is in `app/src/main/assets`, and that copy is the only one. Folders
and file names carry no version numbers. A replaced asset overwrites the file it replaces.

| Location | Contents |
| --- | --- |
| `actor/starter-actors.zip` | The four-view cutout Tavi and the drawn Tavi: actor documents and artwork, with no project frames or performances |
| `actor/presets/views` | Motion presets for each actor view |
| `packs` | Stamp packs: Tavi's cutouts and the shared expressions, eyes, mouths and studio items |
| `samples` | The `Tavi presents` project, its natural-hands copy and their studio background |
| `backgrounds` | Scene backgrounds by category |
| `library` | Demo library items: a stage background and audio clips |
| `fonts` | Text fonts |

The IDs saved inside these files, such as `ls-tavi-cutouts-v6` and `ls-tavi-all-views-v3`, are
stored in people's libraries and projects. They keep their names when a file is replaced. A
changed ID needs a migration (see [file formats](file-formats.md)).

`docs/images` holds the Tavi model and acting sheets, which show how Tavi's hands and face should
look.

## The brand

The mark is a bouncing ball, the first exercise every animator learns and the plainest picture of
squash and stretch. Three frames fall along one diagonal: round near the top, where the ball
moves slowly; stretched and further on as it speeds up; and squashed on the ground. The earlier
frames are carmine, like onion skins on paper, and the landed ball and ground are graphite. The
mark is three solid shapes and a line on a paper tile, so it stays recognisable at 16 px and as a
one-colour themed icon. The diagonal is deliberate: shapes in a triangle read as a face.

The wordmark and the interface use [Nunito](https://github.com/googlefonts/nunito). Its rounded
ends match the pencil look, and its large x-height keeps small labels readable. The wordmark is
Nunito ExtraBold with carmine balls for the dots on the i's.

There is one brand colour, the logo's carmine, with the logo's two neutrals, graphite and paper.
Most leading brands use one or two colours, and an accent only draws the eye while it is scarce
(the 60-30-10 rule: mostly neutral surfaces, some secondary, a little accent). Every interface
colour is carmine or one of its tones: primary is carmine, secondary and tertiary are carmine at
low chroma, and containers are lighter or darker tones, all from carmine's Material HCT palette.
The greys stay neutral so they don't tint the colours on the canvas.

The one colour outside the brand is a drawing aid: the next-frame onion skin on the canvas is
blue, the paper convention, because blue stays distinct from carmine with red-green colour
blindness. Users can change it.

Everything is vector art drawn from one geometry in [`tools/brand`](../tools/brand/README.md):

- the launcher foreground, themed icon and splash in `app/src/main/res/drawable`;
- the in-app mark and wordmark as path data in the designsystem's `BrandArt`;
- `docs/brand`: the brand board, the SVG icon and lockup, and the 512 px store icon.

Change the brand in that geometry and regenerate every file from it, rather than editing one
copy. `BrandResourcesTest` and `BrandThemeTest` check the colours, icons and text contrast.

## Where the artwork came from

The Tavi references, the studio plate and the character artwork were made with AI image
generation for this project, then cut out and assembled into packs and actors. Geometric stamps
were drawn in code. The two `Tavi presents` samples were rendered from the same cutouts by an
authoring script. The natural-hands copy draws Tavi's arms and hands with vector curves in every
frame, following the model sheet, and reuses the original's torso, head, desk, eye and mouth
artwork. The maintainer authorizes Apache-2.0 distribution of all of it.

## Animating the bundled actor

Record purposeful gestures with preparation, movement and settle, and use held poses for pauses
rather than constant bobbing. Change hands through the actor's alternative hand artwork, and use
the shared expression, eye and mouth packs. Keep the mouth at rest during silence.

Check the neck, shoulders, wrists, waist and ankles in neutral and bent poses, and check overlap
order, pivots and skin binding, before blaming the artwork. Verify every artwork change in a
native render and in playback on a phone.

## Contributing assets

Record authorship, original source, license, permitted redistribution and any generation process
with the contribution. Do not copy artwork, voices, music, screenshots or brushes from another
application without compatible permission and attribution. For generated content, record the
tool and process honestly; do not invent an upstream artist or license.

For actor kits, verify every angle and the motion extremes. Parts must overlap correctly at
neck, shoulders, waist, wrists and ankles, with matching pivots and registration. Check native
renders as well as thumbnails. Keep transparent cutouts free of background fringes.

For branding, preserve the approved source pixels. For audio, avoid clipping and check both phone
speakers and headphones.

A change to a shipped pack, actor or sample must keep its saved IDs, and needs a review of the
changed files and a render, import and export check on a phone.
