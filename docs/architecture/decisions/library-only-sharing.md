# Library-only sharing

Accepted by the maintainer on 20 October 2026.

## Context

The app had a Marketplace tab, a placeholder `MarketplaceRepository`, creator and "owned" badges
on library cards, and purchase and entitlement contracts in `core`. None of it was connected to
a server. In an open-source app, an unfinished store raises questions contributors can't answer:
who runs the server, who gets paid, and whether a free build is crippled. It also made the
library code harder to follow, because every picker and card handled two sources.

## Decision

KinetiFrame shares work through files and the local library only:

- Projects are shared as `.kframe` files and stamp packs as `.kpack` files.
- The library stores personal and project-scoped items on the device.
- The Marketplace screen, its navigation tab and Add-menu entries, the placeholder repository,
  creator and ownership metadata, and the `core` commerce contracts are removed.
- Shared browse components move from `ui/components/marketplace` to `ui/components/catalog`.

## Consequences

- The library picker, cards and details sheet have one source and fewer branches.
- No code path suggests paid content, which keeps the app eligible for F-Droid without
  anti-feature labels.
- An online catalog can be proposed later as an optional, separately hosted service. It would
  need its own decision record covering hosting, moderation, licensing of shared assets and
  privacy.
