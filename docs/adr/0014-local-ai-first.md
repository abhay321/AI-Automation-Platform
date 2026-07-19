# ADR 0014: Adopt a Local-AI First Execution Policy

## Status
Accepted

## Date
2026-07-19

## Context
Most current AI automation platforms are built around third-party cloud APIs (e.g., OpenAI, Anthropic, Cohere). While these APIs are powerful, they introduce massive concerns for enterprise users:
1. **Data Privacy**: Sending proprietary financial, healthcare, or client logs to external servers violates standard compliance guidelines (GDPR, HIPAA).
2. **Operational Cost**: High-frequency workflows run against cloud APIs generate massive monthly token bills.
3. **Network Dependability**: Cloud latency variations and server outages can bring critical business automations to a halt.

To resolve these barriers, the platform must allow users to run advanced LLM generation, vector embeddings, and semantic routing fully locally.

## Problem Statement
How should the platform structure its AI execution layer to ensure enterprise privacy, low operational costs, and complete offline capability?

## Decision
Adopt a **Local-AI First Execution Policy**. 

Our primary integrations, templates, and testing pipelines will prioritize running local models via tools like **Ollama** or **LlamaEdge (Wasm)**. We will treat local models as first-class citizens:
- Embedded systems default to using local models (e.g., Llama 3, Mistral, Qwen, Nomic Embed).
- Our vector RAG interfaces default to generating embeddings locally.
- Cloud model APIs (Gemini, OpenAI) are supported as optional configurations rather than mandatory dependencies.

```
+-----------------------------------------------------------+
|                  AI AUTOMATION PLATFORM                   |
+-----------------------------+-----------------------------+
                              |
         +--------------------+--------------------+
         | (Default)                               | (Optional)
+--------v-------+                        +--------v-------+
|  LOCAL MODEL   |                        |  CLOUD API     |
| (Ollama/Llama) |                        | (Gemini/OpenAI)|
+----------------+                        +----------------+
```

## Alternatives Considered
- **Cloud-Only Architecture**: Designing the system strictly around cloud APIs. While easier to implement, it excludes enterprise users with strict compliance requirements and creates high long-term operational costs.

## Advantages
- **Absolute Privacy**: Zero sensitive corporate data leaves the user's host machine or private cloud VPC.
- **Zero Token Fees**: Run massive, high-frequency, continuous loops without incurring per-token usage bills.
- **Reliable Performance**: Execution latency remains fully predictable, untouched by internet throttling or external SaaS outages.

## Disadvantages
- **Hardware Bound**: Local models require substantial system resources (GPU/RAM) to execute efficiently at reasonable speeds. (Mitigated by supporting tiny, optimized models like Phi-3 or Gemma-2b for simple routing tasks).

## Consequences
- The default configurations in `application.yml` target local Ollama and Llama ports.
- Testing workflows will utilize local model setups.

## Risks
- Local model accuracy can be lower than frontier cloud models (like GPT-4). We mitigate this by building highly optimized prompting structures, local RAG caching, and multi-step agent validation loops.

## Migration Strategy
N/A - Standardized in our core templates.

## Future Considerations
Implement automated system check indicators that detect host GPU memory and recommend the optimal local model size (e.g. 2B vs 8B) dynamically.

## Related ADRs
- [ADR 0009: Standardize on Qdrant as the Vector Database](./0009-qdrant-vector-database.md)
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)

## References
- [Ollama Official Repository](https://github.com/ollama/ollama)
- [LlamaEdge Documentation](https://llamaedge.com/)
