# Third-party notices

KinetiFrame's own code and content are licensed under Apache-2.0 (see [LICENSE](LICENSE)). That
license does not cover the libraries KinetiFrame uses or any separately identified third-party
content. Each keeps its own license.

## Libraries

Dependencies and their exact versions are declared in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml) and the module build files. They include:

- AndroidX and Jetpack Compose, including Room
- Kotlin, kotlinx.coroutines and kotlinx.serialization
- Material Components for Android
- Coil
- Compose ColorPicker by skydoves
- Lucide icons for Compose by composables.com
- JUnit, Robolectric and AndroidX Test, used only for tests

Check each project for its license terms.

Build tools and GitHub Actions have their own terms.

Before distributing a binary, list the full resolved dependency graph and include each library's
required notices with the release:

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

This file is not a complete report of transitive dependencies.

## Bundled content

The maintainer has released the bundled artwork, audio, brushes and branding under Apache-2.0.
Where each comes from, and what contributors must provide, is described in the
[asset guide](docs/assets.md). This grant does not cover media a user imports into their own
projects.

Keep the license files in [LICENSES](LICENSES) and any notices inside individual files. Earlier
license grants are described in [NOTICE](NOTICE).
