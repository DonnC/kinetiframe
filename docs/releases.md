# Releases

Successful pushes to `develop` produce development builds and GitHub prereleases. Stable
releases and Play Store updates remain maintainer-controlled. The manifest's `versionName` is not
evidence that a stable release has been published.

## Branches

- **`develop`** is the default branch. Every pull request targets it, including Dependabot
  updates. Each push to it publishes a development build.
- **`main`** holds what is ready to release. It changes only when a maintainer reviews and merges
  `develop` into it. Release tags are made on `main`.
- **`release/vMAJOR.MINOR`** branches are added once releases are stable. Each is cut from `main`
  at a release tag, so older versions can be checked out and patched. Fixes for an old version
  go into its release branch and are also merged into `develop`.

Feature and fix branches are deleted after they are merged.

## Development builds

The Android checks workflow tests and builds the app and sandbox. Each successful matrix job
uploads its debug APK, full commit ID and SHA-256 checksum as an artifact retained for 14 days.
Test reports are uploaded even when tests fail, unless the run is cancelled. Pull requests and
manual runs can produce artifacts but cannot publish a release.

After both matrix jobs pass for a push to `develop`, a separate job publishes the app APK under a
`dev-<full-commit-sha>` prerelease. Names include the commit; rerunning the same commit replaces
its development assets rather than creating another version. These are never marked Latest.
Release assets remain until the maintainer removes them; artifact retention does not remove them.

Development builds install as `com.donnclab.kinetiframe.dev`, alongside the official app. Installations
using the former application IDs have separate private data; it is not automatically migrated.
Export projects from the old installation before removing it. CI uses disposable debug signing
keys, so successive development APKs may require an uninstall before installation. Back up
projects first. These APKs cannot update the Play Store installation.

The publication job downloads only artifacts from its own successful run, verifies their commit
and checksum, and uses the job-scoped GitHub token with `contents: write`. Build/PR jobs retain
read-only repository permissions. No Play account credentials or official signing keys are used.
Repository rules may need to permit Actions to create `dev-*` tags and prereleases.

## Official distribution

The official application ID is `com.donnclab.kinetiframe`. Changing the ID does not migrate an
existing Play listing. Android and Play treat it as a different app; export projects before
removing an older installation. See
[Android application IDs](https://developer.android.com/build/configure-app-module#set-application-id).
Keep the Play signing identity and increase versionCode for each update under the new ID.
Build and review a signed release AAB for Play;
keep the upload key and service-account credentials outside source control. For GitHub stable
releases, distribute an APK signed with the intended distribution key and document whether it
can update a Play-installed copy. Open-source licensing does not require publishing private keys.

## Versioning and changelog

Use `vMAJOR.MINOR.PATCH` Git tags. Use an explicit prerelease suffix such as `-alpha.1` for
unfinished releases. While the app is in development, release notes must call out known limits
and saved-format compatibility. Increase Android `versionCode` for every distributed update.

Maintain [CHANGELOG.md](../CHANGELOG.md) under Unreleased. Before a release, move relevant entries
under the version and date. Describe user-visible changes, fixes, breaking changes and migrations;
do not paste internal refactoring transcripts or claim tests that were not run.

## Release checklist

1. Choose and review the exact commit. Ensure the working tree is clean and CI passes.
2. Update app version fields and changelog. Review resolved dependency licenses and required
   notices, asset provenance and archived-file contents. Check for secrets in the full history.
3. Build the app and sandbox, run host and device tests, and test install/update, drawing,
   save/reopen, import/export, actor playback, audio and camera effects on physical hardware.
4. Test older sample archives and document any migration or incompatibility. Measure memory,
   playback and export behavior on representative hardware; do not infer this from host tests.
5. Build the release artifact using a maintainer-controlled signing key. Never commit the
   keystore, passwords or signing configuration. Keep key recovery outside this repository.
6. Verify the APK's signature, package ID, version and install/update behavior. Produce a SHA-256
   checksum for each distributed artifact.
7. Create a matching tag at the tested commit and a GitHub draft release. Include changelog,
   minimum Android version, known issues, checksums and signing-certificate information.
8. Publish only after reviewing the draft and attached signed artifacts. Debug APKs must be
   labeled development builds, not official signed releases.

Official release signing, stable-tag publication and Play uploads are not automated by the
development workflow. Follow the checklist above for stable distribution.

## Before changing repository visibility

Visibility is changed by the maintainer, not by build tooling. Review all branches, tags,
history, large/media files and attribution before publication; deleting a working-tree file
does not remove it from Git history. Rotate any exposed secret rather than merely deleting it.
Enable GitHub private vulnerability reporting and verify the conduct-reporting contact.
Configure branch protection for the CI checks after they have run on GitHub.
