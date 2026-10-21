# Notes for coding assistants

This file is for AI coding tools. Human contributors should read [CONTRIBUTING.md](CONTRIBUTING.md).
The same rules apply to both.

Before changing code, read:

- [Code guidelines](docs/code-guidelines.md)
- [Architecture](docs/architecture/README.md) and [module boundaries](docs/architecture/modules.md)
- The README of the module you are changing

Then:

- Branch from `develop` and open pull requests against it. Only maintainers merge into `main`.
- Keep changes scoped to the task. Don't reformat or refactor unrelated code.
- Keep files under 500 lines. Functions take at most 6 parameters and constructors 7 (data classes
  are exempt; composables count only required parameters). Never add entries to
  `.architecture/legacy-baseline.properties`.
- Never rename serialized fields or change saved formats without a migration and a compatibility
  test. See [file formats](docs/file-formats.md).
- Don't trade studio drawing space, gestures or accessibility for code structure.
- Run `./gradlew spotlessApply`, then `./gradlew verify` and the owning module's tests. Say which device checks you did not run.
- Put plans and scratch notes in `notes/`, which Git ignores. Don't commit them.
- All project code and assets are Apache-2.0. Keep existing license and third-party notices.
