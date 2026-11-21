# Contributing to the UI

You can improve how KinetiFrame looks and feels without understanding the animation engine. Most
visual work happens in Compose previews with sample data, so you don't need a project, an actor
or even a device to start.

## Where UI code lives

| You want to change... | Look in |
| --- | --- |
| Colors, typography, spacing, shapes | `designsystem/.../designsystem/theme` |
| A reusable control (dock buttons, sliders, chips) | `designsystem/.../designsystem/component` |
| Shared app UI (brand bar, add-flow bars, icon context menus) | `presentation/.../presentation/common` |
| Library cards, details sheets, tabs | `presentation/.../presentation/library/catalog`, `presentation/.../presentation/library` |
| The studio's toolbars, menus and timeline | `presentation/.../presentation/studio/dock`, `presentation/.../presentation/studio/timeline` |
| A screen's layout or behavior | its feature package in `presentation`, for example `presentation/.../presentation/library` |
| Navigation, permissions, Activity setup | `app/.../MainActivity.kt`, `app/.../navigation` |

New shared controls go in `designsystem` when they know nothing
about animation, and in `presentation/.../presentation/common` when they do. When a screen moves out
of `app`, it goes to `presentation/.../presentation/<feature>` together with its state and
ViewModel.

## How to build a control

- **Stateless.** Take values and callbacks as parameters. Don't fetch a ViewModel, read a
  repository, open files or decode images inside a control.
- **Previewable.** Add `@Preview`s with sample data. Cover light and dark themes, and the empty,
  selected, disabled and error states that apply.
- **Accessible.** Icon buttons take a localized content description from the caller. Decorative
  images use `null`. Touch targets should be at least 48dp where the layout allows.
- **Tokens, not literals.** Use `Dimens`, the theme's colors and typography instead of inline
  `dp` values and hex colors.
- **Standard Compose API shape.** Required parameters first, then `modifier: Modifier = Modifier`,
  then optional parameters, then a trailing content lambda if there is one.

Screens are split into a *route*, which collects state from the ViewModel, and a stateless
*screen* that draws it. That keeps the screen previewable.

## The studio is special

The editor is used with fingers and a stylus, often on a small phone, so layout changes there can
hurt real animators. Before changing the studio:

- Keep the canvas as large as possible. Toolbars should be compact and collapsible.
- Keep existing gestures working: tap to select a tool, tap again to open its options, long press
  for menus, two fingers to pan and zoom.
- Test on a real device with a stylus if you can, and include a short recording in the pull
  request.

## Check your work

```sh
./gradlew verify
./gradlew :designsystem:connectedDebugAndroidTest   # interaction tests, needs a device
```

Screenshots and a passing build don't prove a gesture feels right. Say in your pull request what
you tried on a device.

See also the [design-system README](../designsystem/README.md), the
[code guidelines](code-guidelines.md) and the
[design-system decision](architecture/decisions/design-system-boundary.md).
