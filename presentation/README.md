# presentation

Every screen and component of the app, with their ViewModels, as the
[UI in presentation](../docs/architecture/decisions/ui-in-presentation.md) decision describes. It
depends on `core`, `engine` and `designsystem`, never on storage implementations.

| Package | Contents |
| --- | --- |
| `ui` | `BaseViewModel`, which implements the MVI pattern used by every screen: it receives intents, publishes immutable state through a `StateFlow` and sends one-off effects, using `StateStore` from `core` |
| `presentation.actors` | The actor library: cards, the add-actor screen, details sheet and dialogs, their ViewModels, and controls Actor Studio reuses (`rigging`, `sketch`) |
| `presentation.media.audio` | The audio recorder, the trim screen and dialog, preview controls and the clip waveform |
| `presentation.common` | Stateless UI shared by features: the brand mark and brand bar, the add-flow top and bottom bars, icon context menus, sliders and dialogs. The app provides the user's menu label setting through `LocalShowMenuLabels` |

Controls that know nothing about animation belong in [`designsystem`](../designsystem/README.md).
Most screens still live in `app`. Each moves into
`com.donnclab.kinetiframe.presentation.<feature>` together with its route, state mapping and
ViewModel, once its storage access goes through the ViewModel. The
[restructuring tracker](../docs/restructuring.md) lists the order.

```sh
./gradlew :presentation:assembleDebug
./gradlew :presentation:connectedDebugAndroidTest   # needs a device or emulator
```

See [UI contributions](../docs/ui-contributions.md). Licensed under [Apache-2.0](../LICENSE).
