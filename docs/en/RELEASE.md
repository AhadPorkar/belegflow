# Release gates and commercial boundary

Version 0.2.2 is a source/engineering candidate. The following gates are intentionally open; no green badges are fabricated.

| Gate | Required evidence before real customer use |
|---|---|
| Reproducible dependencies | Resolve/review/commit npm lock; verify Maven artifacts; record all transitive dependencies and licenses; pin approved container digests. |
| Networked build | GitHub workflow passes Java 25, Spring Boot, PostgreSQL 18.6, the modern React build and all unit/integration/browser tests. |
| Security | Review threat model, authorize every mutation/download, test tenant escape, CSRF, sessions, password changes, file handling and deployment headers. |
| Documents | Integrate a maintained conformance validator where required; malware/quarantine process; agree unsupported invoice cases. |
| Operations | HTTPS, secret management, least-privilege database roles, off-site encrypted backup and measured restore test, monitoring and incident ownership. |
| Privacy/retention | Review real processing purposes, contracts, subprocessors, retention/erasure/legal-hold procedures and access responsibilities. |
| Accessibility | Automated and manual keyboard/screen-reader/contrast review. WCAG AA conformance is a target, not verified certification. |
| Accounting | Accountant approves generic CSV mapping and tax handling; no DATEV live claim without actual provider integration tests. |
| Commercial model | Confirm customer pain and paid pilot scope, pricing, support, terms, cancellation/data export and onboarding responsibilities. |
| Product ownership | Review generated source, proposed MIT license, branding/trademark availability and portfolio claims. |

**Not implemented:** billing/checkout, email delivery/ingestion, self-service signup, SSO/MFA, full e-invoice conformance, OCR, bank payments, distributed deployment, automated retention or legal holds. These are not hidden behind mock success responses in the production API.

A controlled paid pilot may use manual onboarding and contractual invoicing after the relevant gates, but this bundle alone is not a fully operated subscription business. The original price range was a hypothesis, not market evidence.

## Suggested pilot acceptance

Use redacted/synthetic invoices first. Agree measurable targets for handling time, approval delay, unresolved exceptions and onboarding effort without promising results. Test actual supplier document formats and the accountant's import requirements. Require named ownership for exceptions, second approvals and support. Document what happens when the sole project manager is absent, a second administrator is unavailable, a file is malicious, an import is unsupported or a release needs rollback.

## Security review checklist

No public demo credentials; no leaked `.env`; secure same-origin cookies; no browser-stored access tokens; CSRF on unsafe methods; actual principal revalidation; tenant-scoped read and write checks; creator/second-approver separation; bounded uploads/XML; force-attachment downloads; audit in transactions; safe CSV; idempotent exports; private database; controlled DB privileges; known patch/advisory status. A passing checklist is not an independent penetration test.


## 0.2.0 verification and upgrade requirement

Before a pilot deployment, complete [Testing](TESTING.md), enable strict coverage, review reports and commit the dependency lockfile. The [upgrade diagnosis](TROUBLESHOOTING.md) explains the YAML-only startup fix; do not delete data volumes. Default reminder startup delay is 30 seconds and is configurable with `BF_REMINDER_INITIAL_DELAY_MS`. No schema migration changed. Automated success does not replace the manual security, privacy, accessibility and restore checks above.
