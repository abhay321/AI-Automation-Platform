# ADR 0015: Establish an Open-Source First Engineering Stance

## Status
Accepted

## Date
2026-07-19

## Context
Enterprise integration and automation platforms succeed or fail based on their ecosystem of connectors and community trust. Proprietary SaaS systems suffer from vendor lock-in, arbitrary pricing increases, and lack of transparency. When an enterprise automation breaks due to a obscure internal bug inside a SaaS platform, developers are left helpless, waiting for enterprise support tickets.

By establishing an open-source first stance, we build deep community trust, enable collaborative debugging, and allow developers to audit every line of code running their business processes.

## Problem Statement
What licensing, code structure, and development policies should govern the platform to foster community adoption and contribution?

## Decision
Establish an **Open-Source First Engineering Stance**. 

The entire monorepo—including the core execution engine, SDKs, native Desktop Control Center, first-party plugins, and deployment scripts—will be developed transparently on public code repositories under a permissive open-source license (e.g., Apache 2.0). 

We will prioritize:
- Public, detailed documentation (including this ADR database).
- Transparent roadmap definitions and public GitHub issue tracking.
- Modular structures that enable community members to contribute private/proprietary plugins without modifying core open-source repositories.

## Alternatives Considered
- **Closed-source Core (Open-core)**: Keeping the engine proprietary while open-sourcing only simple connectors. While financially appealing, it slows adoption and degrades trust among security-conscious enterprise developers.

## Advantages
- **Global Contribution**: Community members can fix bugs, create new plugins, and optimize execution structures, accelerating product maturity.
- **Maximum Transparency**: Customers can fully audit the codebase for security compliance and execution integrity.
- **Organic Adoption**: Low barrier to entry for developers who want to download and run the platform locally with zero commercial negotiations.

## Disadvantages
- **Commercial Protection**: Competitors can fork the codebase and offer hosted versions under their own branding. (Mitigated by establishing clear trademark rules and building an exceptional, highly-optimized native desktop and developer experience).

## Consequences
- All code added to this repository must follow clear documentation, licensing, and contribution standards.
- Build chains and dependencies must use purely open-source or permissive packages.

## Risks
- Community-contributed code can introduce security issues. We mitigate this by enforcing rigorous CI pipelines, static code analysis (SAST), and mandatory multi-maintainer approvals on all PRs.

## Migration Strategy
N/A - Direct alignment.

## Future Considerations
Establish an independent Foundation in the future to manage project governance transparently once community scale warrants it.

## Related ADRs
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)
- [ADR 0012: Implement a Plugin-First Architecture](./0012-plugin-first-architecture.md)

## References
- [The Open Source Definition](https://opensource.org/osd)
- [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
