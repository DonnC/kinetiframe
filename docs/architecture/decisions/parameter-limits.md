# Standard parameter limits

Accepted by the maintainer on 20 October 2026.

## Context

The architecture check allowed at most four parameters per function, constructor and lambda,
including Compose `modifier` and content slots. The Compose API guidelines ask components to
accept a `modifier` and optional styling parameters, so most composables broke the rule. The
legacy baseline held 352 entries, and most of them were ordinary Compose code and data classes,
not real design problems. The project also required an `I` prefix on interfaces and an `Impl`
suffix on implementations. That is not Kotlin or Android convention, and none of the existing
interfaces followed it.

## Decision

- Functions and lambdas may take up to six parameters. Constructors may take up to seven. These
  match the defaults of common Kotlin linters.
- Data class constructors are exempt, because their parameters are the data.
- In `@Composable` functions only parameters without a default value count.
- The baseline key still lists every parameter type, so each entry identifies one exact
  signature.
- Interfaces are named after what they are, and implementations after what makes them different.
  The `I` prefix and `Impl` suffix are dropped.

## Consequences

- The legacy baseline drops from 352 to 59 entries. What remains are real cases where a function
  or class does too much.
- Contributors can follow standard Kotlin and Compose style without fighting the checker.
- Grouping parameters into a type is still encouraged when the values belong together. Bundling
  unrelated values into one object to get under the limit is still not allowed.
- The checker's tests cover the new limits, the data class exemption and the Compose rule.
