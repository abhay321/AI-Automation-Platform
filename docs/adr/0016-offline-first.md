# ADR 0016: Adopt an Offline-First Core Topology

## Status
Accepted

## Date
2026-07-19

## Context
Modern cloud-native developers often assume constant, fast internet connections. They build applications that rely on remote databases, cloud configuration storage, external authentication providers, and third-party SaaS logging backends. 

However, this design breaks down when:
1. Developers work in disconnected environments (on planes, in remote locations, or inside secure enterprise offline intranets).
2. Internet connections fail, rendering local tools useless.
3. SaaS vendors go offline, bringing local development workflows to a halt.

To offer an exceptional developer experience, this platform must run fully disconnected from the internet.

## Problem Statement
How should the platform structure its execution engines, UI layers, databases, and dependencies to guarantee 100% functional capability without an active internet connection?

## Decision
Adopt an **Offline-First Core Topology**. 

The entire platform—including backend engine execution, PostgreSQL persistence, Redis caching, MinIO asset storage, Qdrant vectors, and the Compose Desktop interface—must boot, run, compile, and execute with **zero active network connections to external internet servers**. 

We enforce:
- All data repositories default to local loopback addresses (`localhost` or local Docker networks).
- The desktop control center communicates directly with the local Ktor server.
- Remote connections (e.g. cloud models, remote APIs) are isolated behind clear fallback checks.

## Alternatives Considered
- **Cloud-Dependent Local Wrappers**: Building local apps that still proxy their data or execution states to a cloud backend. This was rejected as it fails inside secure offline corporate intranets and violates user privacy.

## Advantages
- **Instant Local Boot**: No external network handshakes or remote authentication loops on startup.
- **Work Anywhere**: Build, test, and run complex AI automations while completely disconnected from the internet.
- **Enterprise-ready Isolation**: Can be deployed directly inside highly secure, fully offline on-premise air-gapped corporate servers.

## Disadvantages
- **Setup Overhead**: Local developer systems must run database and storage services locally. (Resolved by providing profile-based Docker setups that automate this process).

## Consequences
- Core configurations in `application.yml` default exclusively to local loopback hosts.
- The system never sends hidden telemetry or usage tracking pings back to external analytical servers.

## Risks
- Users can run out of local disk storage. We mitigate this by establishing proactive disk monitoring indicators inside our health check frameworks in Phase 2.

## Migration Strategy
N/A - Deeply integrated into Module 1 and Module 2 platform designs.

## Future Considerations
Review data sync options where users optionally elect to back up their local workflows to private cloud repositories securely when internet is available.

## Related ADRs
- [ADR 0014: Adopt a Local-AI First Execution Policy](./0014-local-ai-first.md)
- [ADR 0018: Provide Profile-Based Docker Compose for Local Dev](./0018-docker-compose-development.md)

## References
- [Offline First Design Principles](http://offlinefirst.org/)
- [Developing for Air-Gapped Environments](https://kubernetes.io/docs/tasks/administer-cluster/air-gap/)
