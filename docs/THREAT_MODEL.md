# Threat model / Bedrohungsmodell

Assets: original invoice bytes; identity/session credentials; service evidence; approval decisions; organization boundaries; audit history; exported packages. Trust boundaries: browser-to-server, server-to-database, privileged operations, and downloaded files leaving the product.

| Threat | Implemented mitigation | Residual / required review |
|---|---|---|
| Guessed foreign UUID | Tenant-scoped reads/writes and composite references. | Every new query must preserve scope; no DB RLS is configured. |
| Forged approval | Server role, assignment, evidence, creator and second-signer checks. | Compromised authorized account can still act within its rights; MFA/SSO not implemented. |
| Cross-site mutation | Session-bound CSRF on unsafe methods; same-origin client. | Real TLS/proxy/cookie behavior still requires deployment tests. |
| Concurrent double decision | Row lock, optimistic version, transactional audit. | Human must review after conflict; no blind approval retry. |
| Duplicate original/export retry | Tenant hash uniqueness, serialized import, persisted idempotency. | Different invoices can still represent the same economic expense; review business duplicates. |
| XML entity attack | DTD/entities/external resources/XInclude disabled, bounded uploads. | Not full invoice semantic validation; additional parsing/DoS fuzzing is a release gate. |
| Malicious PDF/image | Signature/size checks and forced attachment. | No malware scanner/quarantine; recipients must not trust content simply because it downloads. |
| Spreadsheet formula | Quoting and formula-prefix neutralization. | Accountant must validate field mapping/tax treatment. |
| Secret/document leakage | No embedded analytics/AI, generic error references, no SQL error text. | Operator access, backups, logs and public demos remain sensitive. |
| DBA tampering | Ordinary updates/deletes blocked on selected immutable tables. | Owner/DBA can bypass; no WORM/cryptographic legal archive claim. |
| Dependency compromise | Explicit manifest versions and CI audit step. | Lockfile, resolved licenses, SBOM, security scans and image/action hash pinning remain open. |

Default read access is organization-wide. Project assignment is not a confidentiality partition. Single-instance sessions and throttling are deliberate limits. Retention/erasure/legal holds and distributed operation require design before adopting additional business requirements.

Deutsch: Schutzgueter und Restgefahren muessen fuer den echten Einsatz neu bewertet werden. Mandantenfilter, Rollen, CSRF, Versionen, sichere XML-Einstellungen und Hashes sind konkrete Kontrollen, aber keine pauschale Sicherheits- oder Rechtszertifizierung. Besonders offen sind Malware-Quarantaene, MFA/SSO, Minimalrechte, Aufbewahrung/Loeschung und die vollstaendige Netzwerkintegration.
