# ADR 0021: Standardize the Security and Encryption Foundations

## Status
Accepted

## Date
2026-07-19

## Context
An automation platform orchestrating corporate workflows must handle highly sensitive data: third-party API tokens, database passwords, workspace access credentials, and private user files. 

If this data is stored in plain-text inside our database, a single security breach or database exposure leaks all integrated SaaS credentials, presenting an existential risk to users. We must protect all credentials and variables at-rest inside the database using strong, modern, standard cryptographic algorithms. 

Furthermore, we need clear abstractions for security tokens and execution contexts to prevent unauthorized access between different workspaces in multi-tenant environments.

## Problem Statement
What cryptographic and security principles should protect secrets-at-rest and govern execution contexts across the platform?

## Decision
Standardize on the following security guidelines from day one:

1. **AES-256-GCM for Encryption At-Rest**: All sensitive credentials, variables, and API keys stored inside PostgreSQL will be encrypted using AES-256 in Galois/Counter Mode (GCM), utilizing unique random initialization vectors (IVs) for every encryption operation to prevent pattern matching.
2. **PBKDF2 with HMAC-SHA256 for Password Hashing**: Any persistent user passwords will be salted and hashed using PBKDF2.
3. **MDC-Scoped Security Context**: All operations will require an active `SecurityContext` scoping the authenticated User, dynamic Workspace scopes, and permission sets, tied to Thread/Coroutine context scopes.
4. **Isolated Credentials Sandbox**: A dedicated `SecretsManager` service will handle cryptographic keys, reading Master Keys exclusively from secure system environments or localized configurations.

## Alternatives Considered
- **Plain-text Database Storage (No encryption)**: Extremely insecure, unviable for production corporate workflows.
- **AES-128-ECB (Electronic Codebook)**: Insecure cryptographic mode; identical plain-text blocks compile into identical encrypted blocks, exposing patterns to analysts.
- **Relying entirely on External Cloud KMS (Google KMS, AWS KMS)**: Prevents local offline installation, violating our core **Offline-First** architecture directive.

## Advantages
- **Absolute Confidentiality**: Even if a database backup is leaked publicly, credentials remain completely safe under AES-256-GCM encryption.
- **Air-Gapped Privacy**: Cryptographic operations occur natively on-host inside the JVM process, requiring no remote network calls to external key management engines.
- **Type-safe Security Abstractions**: Prevents "Workspace Leakage" where user A accesses variables from workspace B due to clear compiler boundaries and strict context filtering.

## Disadvantages
- **Key Loss Risk**: If the Master Encryption Key is lost, encrypted credentials inside the database become completely unrecoverable. This is highlighted as a critical governance requirement in our operations manuals.

## Consequences
- We establish a `SecurityConfig` interface inside our configuration loader requiring an encryption key length of at least 16 characters (or 32 for true AES-256 strength).
- We implement core `SecretsManager` interfaces inside our `platform` module.

## Risks
- Developers could accidentally print encrypted secrets to the application logs. We mitigate this by never allowing credentials models to output plain-text secrets inside their `toString()` implementations.

## Migration Strategy
N/A - Built directly into the baseline schema designs.

## Future Considerations
Add support for hardware security modules (HSM) or local secure enclaves (e.g., TPM, Secure Enclave, Vault) in later phases of enterprise production deployments.

## Related ADRs
- [ADR 0002: Adopt Clean Architecture Principles](./0002-clean-architecture.md)
- [ADR 0016: Adopt an Offline-First Core Topology](./0016-offline-first.md)

## References
- [NIST AES-GCM Specification (SP 800-38D)](https://csrc.nist.gov/publications/detail/sp/800-38d/final)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
