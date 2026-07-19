# Security Policy

We take the security of our platform, runtime engine, sandbox, and customer credentials extremely seriously. This document outlines our reporting channels, response SLAs, and security design boundaries.

---

## 🛡️ Security Design Boundaries

Our platform enforces strict security parameters by design:
1. **API Key Isolation**: Server-side credentials and API keys (e.g., `GEMINI_API_KEY`) must **never** be exposed to browser environments. All client interactions route via backend proxies.
2. **Plugin Sandboxing**: Plugins execute in isolated Classloaders, restricting direct filesystem access, JVM network bindings, or memory reflection unless explicitly declared and permitted in their manifest.
3. **Data Sanitization**: The core Observability pipeline automatically identifies and masks PII, passwords, JWT tokens, and emails prior to writing logs or metrics.

---

## 📞 Reporting a Vulnerability

If you discover a security vulnerability, please report it privately. **Do not disclose security vulnerabilities publicly via GitHub Issues.**

Please email your detailed vulnerability report to **security@aiplatform.org** (or use our private disclosure channels).

### What to Include in Your Report
To help us triage and patch the vulnerability, please include:
* **Description**: Detailed overview of the vulnerability and its potential impact.
* **Proof of Concept (PoC)**: Step-by-step instructions or scripts to reproduce the issue.
* **System Environment**: Version of the platform runtime, operating system, and database adapters used.

---

## ⏱️ Response SLA

* **Initial Acknowledgment**: Within 24 hours of receiving your email.
* **Triage & Severity Score (CVSS)**: Within 72 hours.
* **Public Disclosure / Patch Release**: Typically within 30 days, coordinated under a mutual responsible disclosure timeline.
