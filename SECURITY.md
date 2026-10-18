# Security policy

0.1.0 is an engineering candidate. No security certification or independent penetration test is claimed. Do not publish real invoices, access tokens, database dumps, cookies or environment secrets in issues.

Before making a repository public, the maintainer must configure a private vulnerability-reporting channel (for example GitHub private reporting) and publish its response expectations. This bundle does not invent a monitored email address or security SLA.

The implemented controls include session/CSRF protection, PBKDF2 password hashing, authenticated-principal revalidation, tenant-scoped queries/composite references, role and state checks, version conflicts, safe XML settings, bounded uploads, force-attachment downloads, audit transactions and CSV formula neutralization. Scope and remaining gates are in `docs/en/RELEASE.md` and `docs/en/OPERATIONS.md`.

Demo credentials are public fixtures, not secrets. Nevertheless never expose a demo installation or the browser-local compatibility renderer to real customer data. Dependencies must be resolved, scanned and reviewed in the release environment. Privileged DB operators can bypass application guarantees; immutable-row triggers are not a tamper-proof legal archive.
