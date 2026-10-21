# Design system boundary

Accepted by the maintainer on 19 October 2026.

## Context

Themes and editor controls lived inside app, while presentation held only MVI infrastructure.
UI contributors had to navigate product workflows to change shared appearance. Packages alone
would not prevent a shared control from depending on editor state or storage implementations.

## Decision

Add one Android library, `designsystem`, with package root `com.donnclab.kinetiframe.designsystem`.
It owns Compose appearance and reusable controls and has no project dependencies. App and
presentation may depend on it. Core, engine, data and the engine sandbox may not.

Keep the other module boundaries. There are six production modules and one development sandbox.
Do not introduce separate components, theme, common-ui or per-screen modules. Build tooling
remains in buildSrc and is not an application module or an APK dependency.

The first extraction moves the existing theme and tokens and shares dock controls. Product
screens remain in app until their state and capability dependencies can be moved coherently.
Legacy app adapters preserve existing call sites; they are not a second implementation.

## Alternatives

Keeping everything in presentation avoids another Gradle project but shares its engine/core
classpath with pure visual controls. Splitting several UI libraries adds configuration and
dependency surfaces without independent owners. One design-system library provides the needed
isolation while keeping feature organization package-based.

## Verification and cost

The change adds one Android library configuration, resource namespace and test target. It adds
no provider SDK, new dependency version or engine abstraction. Architecture tests reject product
and ViewModel imports in designsystem and reverse UI dependencies in non-UI modules. Review its
Gradle dependencies as well: source-import checks do not audit every possible runtime behavior.

Verify theme contrast, XML/Compose brand parity, component interactions and app/sandbox builds.
The Kotlin parser normalizes line endings before PSI analysis. Existing CRLF-generated baseline
entries are recognized with reduced parameter counts; the reviewed baseline is not expanded.
Four malformed legacy keys were corrected after checking their live model and editor usages:
FrameManager and TextNode constructors, and the two BaseNode transform overrides. Their limits
decreased or stayed unchanged; designsystem receives no baseline allowances.
Check Studio gestures and visual parity on devices before making unrelated sizing changes.
Measure configuration/build time if this boundary later becomes a bottleneck; do not claim an
unmeasured build-speed improvement.

## References

- [Android modularization patterns](https://developer.android.com/topic/modularization/patterns)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Code guidelines](../../code-guidelines.md)
