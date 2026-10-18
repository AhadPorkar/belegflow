# Module test matrix / Modul-Testmatrix

Version 0.2.1. EN/DE module contracts: [English](../en/MODULE_REFERENCE.md), [Deutsch](../de/MODULE_REFERENCE.md).

The matrix identifies test contracts, not complete coverage. Uploaded 0.2.0 CI evidence confirms 224 frontend tests (92 core + 132 components), type checking and the production build passed; see [observed CI results](CI-106020289651.md). For 0.2.1, 306 domain and 92 frontend core cases were rerun locally. Spring/PostgreSQL/production-E2E execution remains pending. Six new database-free multipart regression cases are supplied but were not executed here. The earlier 17 compatibility-browser checks are historical, not new 0.2.1 results.

| Module / Modul | Backend suites | Frontend suites |
|---|---|---|
| Identity and session security / Identität und Sitzungssicherheit | `IdentityModuleTest, LoginRateLimitFilterTest, IntegrationTest, ModuleIntegrationTest` | `identity.test.jsx; shell-app.test.jsx; modules.test.mjs; http.test.mjs` |
| Suppliers, projects, cost centers and members / Lieferanten, Projekte, Kostenstellen und Team | `CatalogModuleTest, IdentityModuleTest, ModuleIntegrationTest` | `catalog-settings.test.jsx; dialogs.test.jsx; modules.spec.js` |
| Invoice inbox, review and allocation / Rechnungseingang, Prüfung und Zuordnung | `InvoiceModuleTest, InvoiceRepositoryTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest` | `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js` |
| Original ingestion and document preservation / Originalimport und Dokumenterhaltung | `ImportModuleTest, DocumentModuleTest, DomainTest, ModuleIntegrationTest, ConcurrencyIntegrationTest` | `dialogs.test.jsx; http.test.mjs; modules.test.mjs; modules.spec.js` |
| Service-completion evidence / Leistungsnachweise und Abnahme | `DocumentModuleTest, InvoiceModuleTest, IntegrationTest, ModuleIntegrationTest` | `workspace.test.jsx; dialogs.test.jsx; workflows.spec.js` |
| Questions, duplicates and rejection / Rückfragen, Dubletten und Ablehnung | `InvoiceModuleTest, DomainTest, ModuleIntegrationTest` | `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js` |
| Approval policy and delegation / Freigaberegeln und Vertretung | `DomainTest, PolicyMatrixChecks, InvoiceModuleTest, CatalogModuleTest, IdentityModuleTest, IntegrationTest` | `workspace.test.jsx; catalog-settings.test.jsx; dialogs.test.jsx; workflows.spec.js; modules.spec.js` |
| Accounting handoff and idempotency / Buchhaltungsübergabe und Idempotenz | `ExportModuleTest, DomainTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest` | `accounting-audit.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js` |
| Audit history / Audit-Verlauf | `AuditModuleTest, ModuleIntegrationTest, InvoiceModuleTest` | `accounting-audit.test.jsx; modules.test.mjs; modules.spec.js` |
| Reminders and notifications / Erinnerungen und Benachrichtigungen | `NotificationModuleTest, DomainTest, ModuleIntegrationTest` | `catalog-settings.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js` |
| Bootstrap, SQL normalization and configuration / Initialisierung, SQL-Normalisierung und Konfiguration | `BootstrapModuleTest, SharedModuleTest, AdapterTest, JacksonConfigurationTest, MultipartRequestBuilderTest, ModuleIntegrationTest` | `http.test.mjs; shell-app.test.jsx; identity.test.jsx` |

## Exact declared file inventory

The last column concerns the authoring environment only. The 132 component cases below additionally passed in the supplied 0.2.0 CI logs; they are unchanged in 0.2.1.

| Suite file | Expanded cases | Local execution |
|---|---:|---|
| [server/src/test/java/com/belegflow/server/AdapterTest.java](../../server/src/test/java/com/belegflow/server/AdapterTest.java) | 12 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/ConcurrencyIntegrationTest.java](../../server/src/test/java/com/belegflow/server/ConcurrencyIntegrationTest.java) | 3 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/IntegrationTest.java](../../server/src/test/java/com/belegflow/server/IntegrationTest.java) | 15 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/MultipartRequestBuilderTest.java](../../server/src/test/java/com/belegflow/server/MultipartRequestBuilderTest.java) | 6 | Added in 0.2.1; not executed here |
| [server/src/test/java/com/belegflow/server/ModuleIntegrationTest.java](../../server/src/test/java/com/belegflow/server/ModuleIntegrationTest.java) | 29 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/audit/AuditModuleTest.java](../../server/src/test/java/com/belegflow/server/audit/AuditModuleTest.java) | 4 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/bootstrap/BootstrapModuleTest.java](../../server/src/test/java/com/belegflow/server/bootstrap/BootstrapModuleTest.java) | 4 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/catalog/CatalogModuleTest.java](../../server/src/test/java/com/belegflow/server/catalog/CatalogModuleTest.java) | 17 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/config/JacksonConfigurationTest.java](../../server/src/test/java/com/belegflow/server/config/JacksonConfigurationTest.java) | 4 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/documents/DocumentModuleTest.java](../../server/src/test/java/com/belegflow/server/documents/DocumentModuleTest.java) | 11 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/documents/ImportModuleTest.java](../../server/src/test/java/com/belegflow/server/documents/ImportModuleTest.java) | 8 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/exports/ExportModuleTest.java](../../server/src/test/java/com/belegflow/server/exports/ExportModuleTest.java) | 10 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/identity/IdentityModuleTest.java](../../server/src/test/java/com/belegflow/server/identity/IdentityModuleTest.java) | 14 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/identity/LoginRateLimitFilterTest.java](../../server/src/test/java/com/belegflow/server/identity/LoginRateLimitFilterTest.java) | 4 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/invoices/InvoiceModuleTest.java](../../server/src/test/java/com/belegflow/server/invoices/InvoiceModuleTest.java) | 21 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/invoices/InvoiceRepositoryTest.java](../../server/src/test/java/com/belegflow/server/invoices/InvoiceRepositoryTest.java) | 6 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/notifications/NotificationModuleTest.java](../../server/src/test/java/com/belegflow/server/notifications/NotificationModuleTest.java) | 9 | Not executed in the authoring environment |
| [server/src/test/java/com/belegflow/server/shared/SharedModuleTest.java](../../server/src/test/java/com/belegflow/server/shared/SharedModuleTest.java) | 8 | Not executed in the authoring environment |
| [frontend/tests/components/accounting-audit.test.jsx](../../frontend/tests/components/accounting-audit.test.jsx) | 9 | Not executed in the authoring environment |
| [frontend/tests/components/catalog-settings.test.jsx](../../frontend/tests/components/catalog-settings.test.jsx) | 21 | Not executed in the authoring environment |
| [frontend/tests/components/dialogs.test.jsx](../../frontend/tests/components/dialogs.test.jsx) | 39 | Not executed in the authoring environment |
| [frontend/tests/components/identity.test.jsx](../../frontend/tests/components/identity.test.jsx) | 9 | Not executed in the authoring environment |
| [frontend/tests/components/shell-app.test.jsx](../../frontend/tests/components/shell-app.test.jsx) | 26 | Not executed in the authoring environment |
| [frontend/tests/components/workspace.test.jsx](../../frontend/tests/components/workspace.test.jsx) | 28 | Not executed in the authoring environment |
| [frontend/tests/e2e/modules.spec.js](../../frontend/tests/e2e/modules.spec.js) | 10 | Not executed in the authoring environment |
| [frontend/tests/e2e/workflows.spec.js](../../frontend/tests/e2e/workflows.spec.js) | 4 | Not executed in the authoring environment |

`DomainTest` supplies 306 dynamic cases. `frontend/tests/unit/*.test.mjs` supplies 92 cases. Node and Vitest run the same core case bodies; do not count those twice as separate business specifications. The standalone browser script adds 17 limited compatibility checks, not real-server E2E proof.

Security/transaction negative cases include cross-organization UUIDs, wrong roles, invalid/missing CSRF, session revocation, stale invoice versions, self-approval, missing evidence, unresolved questions, invalid dates, duplicate imports, export replay and immutable database rows.

The 224-case role/state/action matrix uses a fully ready fact fixture to isolate role/stage rules. Additional original domain cases exercise missing/invalid facts, time boundaries, monetary rules, safe XML/file processing, CSV and duplicates. It is not a combinatorial proof of every possible input.
