# presentation

Shared ViewModel infrastructure and the app UI that several features use. It depends on `core`,
`engine` and `designsystem`, never on storage implementations.

| Package | Contents |
| --- | --- |
| `ui` | `BaseViewModel`, which implements the MVI pattern used by every screen: it receives intents, publishes immutable state through a `StateFlow` and sends one-off effects, using `StateStore` from `core` |
| `presentation.common` | Stateless UI shared by features: the brand mark and brand bar, the add-flow top and bottom bars, icon context menus, sliders and dialogs. The app provides the user's menu label setting through `LocalShowMenuLabels` |

Controls that know nothing about animation belong in [`designsystem`](../designsystem/README.md).
Screens still live in `app`. When a screen moves here, it goes into
`com.donnclab.kinetiframe.presentation.<feature>` together with its route, state mapping and
ViewModel, once those no longer depend on app-only classes.

```sh
./gradlew :presentation:assembleDebug
./gradlew :presentation:connectedDebugAndroidTest   # needs a device or emulator
```

See [UI contributions](../docs/ui-contributions.md). Licensed under [Apache-2.0](../LICENSE).
