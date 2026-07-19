# Open Source Governance & Contribution Guidelines

Thank you for your interest in contributing to our **AI Automation Platform**! This document outlines our open-source governance policies, development standards, and code review flows.

---

## ⚖️ Code of Conduct

We are committed to providing a welcoming, safe, and professional environment for everyone. By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## 🛠️ How to Contribute

### 1. Discuss Before Coding
For any major change, new platform capability, or breaking API modification, you must open a GitHub Discussion or submit an [RFC](docs/RFC_PROCESS.md) draft PR before writing implementation code. This ensures alignment with our performance and core scope bounds.

### 2. Follow Our Coding Standards
* **Language**: All core platform code is written in **TypeScript** for the visual workspace and **Kotlin** for the backend runtime modules.
* **Low Allocation**: Avoid instantiating heavy framework dependency injection pools. Keep core abstractions lightweight and coroutine-native.
* **Typing**: Keep types clean, rigid, and complete. Avoid `any` in TypeScript or raw type parameters in Kotlin.
* **Logging & Telemetry**: Never use stdout (`println`). All messages must route through the unified `PlatformLogger` carrying trace and correlation headers.

### 3. Submission Flow
1. Fork the repository and create a feature branch (`feature/your-feature`).
2. Implement your changes, writing high-quality unit and integration tests.
3. Run the verification tools:
   * **TypeScript**: `npm run lint` and `npm run build`
   * **Kotlin**: `./gradlew build`
4. Open a Pull Request, referencing any related issues or approved RFCs.
5. Address review feedback promptly. All PRs require approval from at least one core owner and successful build compilation.

---

## 🔐 Security Disclosures

If you discover a security vulnerability, please **DO NOT** open a public issue. Review our [Security Policy](SECURITY.md) to report vulnerabilities privately.

---

## 🤝 Governance Model

The platform is managed under a **Benevolent Dictatorship / Core Maintainer Group** structure:
* **Core Maintainers**: Responsible for reviewing RFCs, merging Pull Requests, protecting master branches, and cutting official releases.
* **Contributors**: Submit code improvements, bug fixes, updates to documentation, and help answer community questions.
* **Steering Committee**: Convenes quarterly to evaluate the [Roadmap](docs/ROADMAP.md) and align capabilities with enterprise standards.
