# Glossary

KinetiFrame borrows words from both animation and software. This page explains what each term
means in this project, so animators and developers mean the same thing when they use it. Code
names are shown in `monospace` where they differ from the word used in the app.

## Projects and drawing

- **Project**: One animation: its stage size, frame rate, frames, layers, actors, audio, video and camera moves. Saved on the device as a folder and shared as a `.kframe` file.
- **Stage**: The visible canvas area of a project, with a fixed width and height in pixels.
- **Frame**: One picture in the timeline. At 12 fps, twelve frames make one second.
- **Layer**: A transparent sheet stacked with others in each frame. Drawing on one layer never changes another.
- **Held drawing** (also called a cel): A single drawing shown across several frames instead of being redrawn on each one. Animators call this "shooting on twos" or holding a pose.
- **Onion skin**: Faint copies of the previous and next frames shown behind the current one, so you can see how the drawing moves.
- **Stamp pack** (`Pack`, `.kpack`): A named set of small drawings, such as mouth shapes or hand poses, that you can drag onto any frame again and again. Packs live in your library and can be shared as `.kpack` files.
- **Library**: Your own collection of reusable items stored on the device: projects, actors, performances, packs, audio, backgrounds and palettes. Items can be **personal** (available in every project) or **project-scoped** (stored inside one project and shared with it).

## Characters

- **Actor**: A character that can be posed and animated without redrawing it. An actor has a rig and artwork, and can be placed into any number of projects.
- **Rig** (`SceneGraph`): The skeleton inside an actor: a tree of bones connected at joints, such as torso, upper arm, forearm and hand.
- **Bone**: One segment of a rig. Each bone has a length, a pivot point and a rotation relative to its parent.
- **Inverse kinematics (IK)**: Posing by moving the end of a limb and letting the joints above it follow, like dragging a hand and having the elbow and shoulder bend. KinetiFrame uses FABRIK and analytic two-bone solvers.
- **Cutout artwork**: Separate pictures attached to individual bones, like a paper puppet.
- **Continuous drawing** (skinning): One whole drawing bound to several bones so it bends smoothly at the joints instead of showing separate pieces.
- **View**: A separately drawn direction of the same actor: front, back, profile or three-quarter. Switching views swaps artwork; it is not automatic 3D rotation.
- **Artwork variant**: An alternative picture for one part, such as an open hand or a fist, that can be switched during a performance.
- **Revision**: A saved version of an actor. Projects pin the exact revision they were made with, so editing an actor later never breaks an older scene.

## Motion

- **Performance**: A recorded piece of acting for one actor: the poses, artwork switches and movement over time. Performances can be reused across projects.
- **Take**: One recording attempt while creating a performance. You can review takes and keep the best one.
- **Root motion**: Movement of the whole character across the stage, such as walking from left to right, as opposed to movement of its limbs.
- **Motion preset**: A ready-made movement that can be applied to an actor and then adjusted, instead of recording it by hand.
- **Placement** (`ActorPlacement`): One appearance of an actor in a project: which actor and revision, which performance, and where and when it appears on the timeline.
- **Cast view**: A preview showing an actor from all its authored views at once, used to check that the views match.

## Scene and output

- **Camera move** (`CameraShot`): A timed camera effect over a range of frames, such as a push in, pan, shake or fade.
- **Export**: Rendering a project to an MP4 video. The video cannot be edited again, so keep the `.kframe`.
- **`.kframe`**: A shareable project file. It is a ZIP archive containing the project and every actor it uses. See [file formats](file-formats.md).
- **`.kpack`**: A shareable stamp pack file. A ZIP archive with a `meta.json` and the pack's PNG drawings.

## Code terms

- **Studio**: The main editing screen. Much of its state still lives in classes named `Timeline*`, such as `StudioViewModel`, `StudioState` and `StudioIntent`, which cover the whole editor, not only the timeline strip.
- **Actor Studio**: The editor for building rigs, attaching artwork and recording performances.
- **MVI**: Model-View-Intent. Screens send *intents* (user actions) to a ViewModel, which publishes an immutable *state* and one-off *effects*. See [architecture](architecture/README.md).
- **Loose Signal**: The name of the bundled sample art set, including the Tavi, Mira and Kuda characters. Its sources and build scripts are under `docs/art-direction/loose-signal` and `tools/loose_signal`.
- **Legacy baseline**: The list in `.architecture/legacy-baseline.properties` of older code that breaks the size or parameter rules. It can only shrink. See [code guidelines](code-guidelines.md).
