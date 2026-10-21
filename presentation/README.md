# presentation

Shared ViewModel infrastructure. `BaseViewModel` implements the MVI pattern used by every screen:
it receives intents, publishes immutable state through a `StateFlow` and sends one-off effects,
using `StateStore` from `core`. It depends on `core`, `engine` and `designsystem`, never on
storage implementations.

Screens still live in `app`. When a screen moves here, it goes into
`com.donnclab.kinetiframe.presentation.<feature>` together with its route, state mapping and
ViewModel, once those no longer depend on app-only classes. Shared visual controls belong in
[`designsystem`](../designsystem/README.md), not here.

See [UI contributions](../docs/ui-contributions.md). Licensed under [Apache-2.0](../LICENSE).
