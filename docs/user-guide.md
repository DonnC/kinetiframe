# Using KinetiFrame

This guide walks through making your first scene. If a word is unfamiliar, check the
[glossary](glossary.md).

## Your first scene

1. **Create a project** from the home screen, or open the bundled *Tavi presents* example to see
   a finished scene.
2. **Draw.** Pick a brush, draw on the canvas, and add frames in the timeline. Use layers to keep
   the background, characters and effects apart. Turn on onion skinning to see neighboring
   frames while you draw.
3. **Add a character.** Open the Add menu, choose *Actor performance* and pick a character. The
   bundled characters (Tavi, Mira and Kuda) are a good place to start before building your own.
4. **Make it move.** Pick one of the character's saved performances, or create a new one in Actor
   Studio by moving the character's parts while recording. Watch the take back before you keep it.
5. **Add sound.** Import audio or record a voice-over. Scrub the timeline to line up sound with
   the drawing.
6. **Add a camera move** such as a push in or a pan over the frames where you want it.
7. **Play the whole scene** from the first frame, then save.
8. **Share it.** Export a video, or share the editable `.kframe` project file.

Keep the `.kframe` even after exporting a video. The video can't be edited again, and the project
file includes the actors it uses.

## Characters

There are two ways to give an actor its artwork:

- **Cutout:** separate drawings attached to each body part, like a paper puppet.
- **Continuous drawing:** one whole drawing that bends at the joints.

Front, back, profile and three-quarter views are drawn separately. Switching views swaps artwork;
it doesn't rotate the character in 3D.

Before recording, line up the joints with your artwork. Then check the neck, shoulders, waist,
wrists and feet in a few strong poses, not just standing still. Imported artwork often needs its
position, size or pivot point adjusted.

## The library

The library keeps things you want to reuse: actors, performances, stamp packs, audio, backgrounds
and whole projects. Items can be **personal**, so they are available in every project, or saved
to **this project** only, so they travel with the `.kframe` when you share it.

Stamp packs can be shared on their own as `.kpack` files.

## Audio, video and export

Play the whole scene from the start after changing any timing. For long projects, export a short
test first to check the result on your device. Which audio and video formats work depends on your
phone's built-in codecs.

KinetiFrame stores everything on your device and works offline. Keep the original audio and video
files until your project is backed up.

## Something went wrong?

KinetiFrame is still young. If drawing, rigging, playback or export misbehaves, please
[open an issue](https://github.com/DonnC/kinetiframe/issues/new/choose) with your app version,
device, Android version and the steps you took. A short screen recording helps a lot. See
[contributing](../CONTRIBUTING.md) for what else is useful, and [security](../SECURITY.md) for
reporting vulnerabilities privately.
