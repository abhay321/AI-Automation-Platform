# ADR 0010: Standardize on MinIO for Object Storage

## Status
Accepted

## Date
2026-07-19

## Context
AI automations consistently read and write unstructured binary assets: raw transcripts, video files, crawled web pages, model weights, PDF documents, and system images. 

Relational databases are terrible at storing raw files (BLOBs degrade database transactions and bloat backups). Keeping files on a raw local server filesystem works for local development but fails completely in cloud clustered environments, where stateless server processes cannot access each other's local disk arrays.

We need a unified, enterprise-grade, S3-compatible Object Storage system that can run locally offline and transition seamlessly to enterprise cloud solutions (like AWS S3, Google Cloud Storage, or Azure Blob Storage).

## Problem Statement
What technology should handle raw asset file storage across local development and production environments?

## Decision
Standardize on **MinIO** as the object storage platform. 
MinIO is a lightweight, high-performance, S3-compatible server that can be booted locally in a single container or deployed across clustered Kubernetes systems. Our integration will use the standard **AWS S3 Java/Kotlin Client**, meaning our code remains 100% cloud-agnostic.

## Alternatives Considered
- **Direct Local Filesystem Storage**: Simple for local dev, but fails to scale, lacks clustering support, and creates vendor-lock on local hardware setups.
- **Direct Cloud APIs (AWS S3 SDK, GCS SDK directly)**: Prevents local offline execution, violating our core **Offline-First** requirement.
- **Database BLOBs**: Extremely bad practice that slows PostgreSQL backups, increases database transaction locks, and degrades database performance.

## Advantages
- **S3 API Compatibility**: MinIO utilizes the exact S3 API standard. This means our platform can switch from MinIO (local dev / on-premise) to AWS S3 (production) simply by changing a configuration variable.
- **High-Performance**: Ultra-low latency file reads and writes.
- **Clean S3 Java SDK**: Exceptionally mature, robust libraries with advanced streaming, multi-part uploads, and security configurations.
- **Integrated Browser UI**: MinIO includes a beautiful built-in console for exploring buckets and downloading files.

## Disadvantages
- **Additional Local Service**: Requires running the MinIO service locally. (Sufficiently resolved by defining a standard Docker profile).

## Consequences
- The core platform accesses files via an `AssetRepository` interface.
- Implementations of this interface will interact with S3-compatible buckets using pre-signed URLs or stream channels.

## Risks
- Loose bucket configurations can expose sensitive workflow attachments. We mitigate this by establishing strict default private policies and accessing files strictly via short-lived, pre-signed URLs.

## Migration Strategy
N/A - Direct integration into the platform asset system.

## Future Considerations
Implement bucket automatic cleanup and retention policies inside the platform configuration framework to automatically purge transient workflow files.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)

## References
- [MinIO Object Store Official Site](https://min.io/)
- [AWS SDK for Java - S3 Examples](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3.html)
