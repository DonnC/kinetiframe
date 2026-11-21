# Roadmap

This is where KinetiFrame is heading. It's a plan, not a promise: priorities change as people
use the app and new contributors arrive. If you want to work on something here, comment on the
matching issue or open one.

## Now: feature modules

Splitting `app` into feature modules, so the app builds faster, boundaries are enforced by the
build, and a screen's look can change without touching the rules behind it. The
[restructuring tracker](docs/restructuring.md) lists what's done, what's in progress and what's
next, with the debt counts that show progress.

## Next: polish and the first public release (v0.1.0)

- **UX fixes** found on device checks, listed in the [restructuring tracker](docs/restructuring.md#ux-fixes):
  readable Actor Studio controls, content kept clear of the system bars, and fewer interrupting
  dialogs.
- **Translations.** Move the remaining user-facing text into Android string resources, then open
  the app to translators.
- Publish a tagged `v0.1.0` release with signed APKs and release notes.
- Add screenshots and a short demo clip to the README and user guide.
- Prepare an F-Droid listing. KinetiFrame has no proprietary dependencies, so this mainly needs
  store metadata and a reproducible release build.

## Then: easier to contribute to

- **Public API docs** for `core` and `engine`, generated with Dokka.
- **Cross-platform asset tools.** Port the PowerShell asset builders in `tools/loose_signal` to
  Python so they run on Linux and macOS too.

## Later: for animators

- A longer user guide with step-by-step tutorials for frame-by-frame animation and actors.
- More sample actors, stamp packs and backgrounds from community artists.
- Better performance editing: trimming takes and adjusting timing after recording.
- Tablet layouts and more stylus settings.
- A published, versioned specification of the `.kframe` and `.kpack` formats, so other tools
  can read and write them.

## Not planned right now

- **An online marketplace.** Sharing works through `.kframe` and `.kpack` files and the local
  library. An online catalog may be revisited later as a separate, optional service.
- **Accounts, ads, analytics or in-app purchases.**
- **A general-purpose video editor.** KinetiFrame is for making animation.

## Good first tasks

These are small, well-scoped and a good way to learn the codebase. Each will have a
[`good first issue`](https://github.com/DonnC/kinetiframe/labels/good%20first%20issue).

- Move the strings in one screen (for example Settings) into `strings.xml`, and lower its count in
  `.architecture/ui-values-baseline.properties`.
- Replace one screen's inline colours or sizes with `designsystem` tokens.
- Add a missing Compose preview for a reusable control, including its disabled state.
- Add a unit test for an untested engine function.
- Improve a section of the user guide, with screenshots.
- Remove one entry from `.architecture/legacy-baseline.properties` by grouping a function's
  parameters into a well-named type.
