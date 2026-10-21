# Security policy

## Reporting a vulnerability

Do not post exploit details, private projects, credentials or personal data in a public issue.
Use GitHub's “Report a vulnerability” option on the repository's Security tab.
If that option is unavailable, open an issue requesting a private reporting channel,
without including vulnerability details.

Include the affected commit or version, device and Android version, reproduction steps,
impact and a minimal non-sensitive sample. Reports are reviewed by the maintainer;
there is no guaranteed response time or bounty.

## Supported versions

Security fixes target the default branch until a supported release series is announced.
Development builds and older tags do not have a separate maintenance commitment.

## Scope and expectations

This policy covers the Android app, engine, persistence, import/export, bundled tooling
and build/release configuration. Imported project archives, media and asset packs are
untrusted inputs.

Review archive path confinement, bounded decoding, permission handling, safe file replacement,
secret exposure and release-artifact integrity. A malformed import must not write outside
its destination or silently overwrite unrelated user data.

There are no blanket finding exclusions or accepted-risk exceptions in this policy.
Review claims against reachable code and realistic impact. Tests and architecture checks
are not a security audit.
