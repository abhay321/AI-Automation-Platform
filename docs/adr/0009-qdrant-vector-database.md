# ADR 0009: Standardize on Qdrant as the Vector Database

## Status
Accepted

## Date
2026-07-19

## Context
Retrieval-Augmented Generation (RAG) is core to modern AI automation. Nodes must convert documents, chat logs, transcripts, and enterprise files into high-dimensional vector embeddings, index them, and run high-speed cosine similarity searches to feed relevant memory context back to LLM prompts. 

We need a dedicated, extremely fast vector search database that runs beautifully locally (using low resource footprints) and scales up to handle billions of vector records in production environments.

## Problem Statement
Which vector database should be integrated as the standard repository for RAG pipelines, text memories, and similarity searches?

## Decision
Standardize on **Qdrant** as our primary Vector Database. 
Qdrant is written in Rust, provides exceptionally high performance, has a low resource profile, and exposes native, type-safe gRPC and REST APIs.

## Alternatives Considered
- **pgvector (PostgreSQL Extension)**: Excellent for simple integrations, but lacks advanced filtering structures, suffers from performance degradation under rapid dynamic updates, and complicates database-level memory allocations.
- **Pinecone**: Fully managed SaaS product. Violates our **Local-First / Offline-First** design philosophies.
- **Chroma**: Excellent for local python scripting, but lacks enterprise cluster scalability and lacks robust multi-tenant security configurations.
- **Milvus / Weaviate**: Robust enterprise databases, but exceptionally heavy, resource-intensive on local machines (often requiring multiple sub-containers), complicating simple developer workstation setups.

## Advantages
- **Rust-powered Performance**: Fast, extremely reliable memory usage, and outstanding search latency.
- **Local-First & Production-Scalable**: Can be booted locally inside a single small Docker container or scaled as a fully distributed Kubernetes cluster.
- **Advanced Filtering**: Supports complex payload filtering (e.g., retrieving embeddings filtered by `workspace_id = X` and `created_at > date`) during the vector search itself, avoiding post-filtering overhead.
- **Official Java/Kotlin SDK**: Provides strong, type-safe gRPC connectors out-of-the-box.

## Disadvantages
- **Additional Dependency**: Adds Qdrant to our infrastructure stack. (Mitigated by making Qdrant integrations lazy-loaded and setting up automatic docker-compose profiles).

## Consequences
- Embeddings nodes will serialize data directly to the active Qdrant instances.
- Developers can configure collection creation policies inside the admin panel.

## Risks
- Massive vector sets consume substantial RAM. We mitigate this by establishing clean collection indexing standards and configuring disk-backed payloads.

## Migration Strategy
We will provide standard setup scripts that generate standard collection configurations automatically at startup.

## Future Considerations
Ensure collection indexing policies are abstracted inside our `VectorStoreRepository` to permit seamless swapping to other vector engines if requested by the user.

## Related ADRs
- [ADR 0014: Adopt a Local-AI First Execution Policy](./0014-local-ai-first.md)
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)

## References
- [Qdrant Vector Database Official Site](https://qdrant.tech/)
- [Qdrant Java/gRPC Client](https://github.com/qdrant/java-client)
