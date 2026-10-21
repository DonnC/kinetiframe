# designsystem

KinetiFrame's look and feel: the Compose theme, design tokens and reusable controls. It has no
project dependencies, ViewModels, storage or animation code, so you can work on it with previews
and sample values alone.

## What's here

- `theme`: colors, typography, dimensions and the editor, card and panel tokens.
- `component`: reusable controls, one per file, each with previews.
- `src/androidTest`: interaction tests written with `createComposeRule()`.

## Rules for controls

- Take values and callbacks from the caller. Don't read preferences, open files, decode artwork
  or create engine objects.
- Accessibility labels come from the caller and are localized. Decorative icons have no content
  description.
- Feature-specific meaning belongs in the screen that uses the control, not in the control.

`DockToolButton` follows the studio's interaction pattern: tap an unselected tool to select it,
tap the selected tool to open its options, and long press or double tap to open the menu.
`DockDragHandle` only draws the grip; the caller handles dragging. Some existing studio controls
are smaller than the recommended 48dp touch target. Changing their size is a UX decision to
discuss in an issue first.

## Workflow

Open a component's `@Preview` in Android Studio and edit against the sample values. Cover the
states that matter, such as selected, disabled and empty. Theme tests check text contrast, and app
resource tests check that launcher and splash colors match the theme.

```sh
./gradlew :designsystem:testDebugUnitTest :designsystem:assembleDebug
./gradlew :designsystem:connectedDebugAndroidTest   # needs a device or emulator
```

These need the full build. The engine-only build (`-PengineOnly=true`) leaves this module out.

See [UI contributions](../docs/ui-contributions.md) and the
[design-system decision](../docs/architecture/decisions/design-system-boundary.md).
Licensed under [Apache-2.0](../LICENSE).
