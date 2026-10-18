# Module reference and extension contracts

Version 0.2.1 | [English](../en/MODULE_REFERENCE.md) | [Deutsch](../de/MODULE_REFERENCE.md)

These boundaries describe the implemented application, not a future ERP. Paths are relative to the repository; server package files live under `server/src/main/java/com/belegflow/server/`, domain classes under `domain/src/main/java/com/belegflow/domain/`. The API reference gives payloads; source gives exact signatures.

[API](../api/API.md) | [Code map](../code/CODE_MAP.md) | [Tests](TESTING.md)

## 1. Identity and session security

**Source**: `identity/IdentityService.java`; `identity/SecurityConfiguration.java`; `identity/SecurityPrincipal.java`; `identity/LoginRateLimitFilter.java`; `identity/SessionController.java`.

**API**: `/api/csrf, /api/login, /api/logout, /api/session, /api/session/locale, /api/public/config`.

**Data**: `app_user, organization; server session; no client-selected tenant`.

Resolve the active account and authentication version on each protected operation. Trust the server principal, not a submitted role or organization UUID. Rotate login/session state, require CSRF on unsafe requests, throttle direct login attempts, and return stable error codes. Locale may change only for the current account.

**Use and extend**: Start with login, session revocation and cross-organization negative cases. Adding SSO requires an explicit principal mapping and session/security review, not bypassing IdentityService.

**Backend tests**: `IdentityModuleTest, LoginRateLimitFilterTest, IntegrationTest, ModuleIntegrationTest`.

**Frontend tests**: `identity.test.jsx; shell-app.test.jsx; modules.test.mjs; http.test.mjs`.

## 2. Suppliers, projects, cost centers and members

**Source**: `catalog/CatalogService.java`; `catalog/CatalogController.java`.

**API**: `/api/catalog, /api/suppliers, /api/projects, /api/cost-centers, /api/users, /api/settings/rules`.

**Data**: `supplier, project, cost_center, app_user, organization`.

Finance/Admin manages financial catalogs; only Admin changes team members and approval thresholds. An assigned project manager must be an active eligible approver in the same organization. Updating an account changes its authentication version; an administrator cannot deactivate or demote itself. Creation and audit are transactional.

**Use and extend**: Create a supplier, project and cost center before allocating a new invoice. Add a new catalog field to the DTO, validation, SQL migration, API docs, both forms and positive/negative tests together.

**Backend tests**: `CatalogModuleTest, IdentityModuleTest, ModuleIntegrationTest`.

**Frontend tests**: `catalog-settings.test.jsx; dialogs.test.jsx; modules.spec.js`.

## 3. Invoice inbox, review and allocation

**Source**: `invoices/InvoiceRepository.java`; `invoices/InvoiceService.java`; `invoices/InvoiceController.java`.

**API**: `/api/invoices, /api/invoices/summary, /api/invoices/{id}, /api/invoices/{id}/allocation, /api/invoices/{id}/commands`.

**Data**: `invoice plus tenant-scoped supplier/project/user joins`.

Lists and details are tenant-scoped and paged. A UUID is not an authorization grant. Mutations lock the invoice and compare the expected version before applying a policy. Allocation is allowed only in editable stages, and a project change invalidates earlier evidence acceptance. Review does not rewrite original financial data.

**Use and extend**: Use the returned allowed_actions/readiness facts for presentation, while rechecking on the server. Add commands rather than writable arbitrary statuses; extend the role/state/action matrix and concurrent-version test.

**Backend tests**: `InvoiceModuleTest, InvoiceRepositoryTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend tests**: `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js`.

## 4. Original ingestion and document preservation

**Source**: `documents/InvoiceImportService.java`; `documents/DocumentService.java`; `documents/DocumentController.java`.

**API**: `/api/invoices/import, /api/documents/{id}`.

**Data**: `document (bytea, SHA-256, metadata), invoice, supplier`.

Bound file bytes before parsing. Secure UBL/CII extraction is not full standards validation. PDF fields are entered manually. Missing/malformed dates are validation errors. Exact-file reimport reuses the existing invoice; a different original with the same business key creates a warning. Preserve original bytes and use forced, scoped downloads.

**Use and extend**: A new document family needs bounded safe parsing, file-policy tests, a supported-data contract and negative fixtures. Do not enable DTDs or treat a MIME header as sufficient verification. An external archive needs separate retention/immutability guarantees.

**Backend tests**: `ImportModuleTest, DocumentModuleTest, DomainTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend tests**: `dialogs.test.jsx; http.test.mjs; modules.test.mjs; modules.spec.js`.

## 5. Service-completion evidence

**Source**: `documents/DocumentService.java`; `invoices/InvoiceService.java`.

**API**: `/api/invoices/{id}/evidence, /api/invoices/{id}/evidence-acceptance`.

**Data**: `document(kind=EVIDENCE), invoice acceptance fields, audit_event`.

Require a real attached file and the responsible project manager or active delegate. The creator cannot accept its own required service evidence. New evidence or a project change resets acceptance. Acceptance is a business fact separate from the later invoice approval, not a qualified digital signature.

**Use and extend**: Finance attaches a report; the independent project owner downloads and checks it before accepting. Preserve file-count, size, version and role checks when extending evidence types.

**Backend tests**: `DocumentModuleTest, InvoiceModuleTest, IntegrationTest, ModuleIntegrationTest`.

**Frontend tests**: `workspace.test.jsx; dialogs.test.jsx; workflows.spec.js`.

## 6. Questions, duplicates and rejection

**Source**: `invoices/InvoiceService.java`; `domain/WorkflowPolicy.java`; `domain/DuplicateFingerprint.java`.

**API**: `/api/invoices/{id}/comments, /questions/{question}/resolution, /duplicate-resolution, /commands`.

**Data**: `discussion, invoice duplicate/readiness/approval fields, audit_event`.

A comment does not resolve a question. Clarification creates an open question and resets signatures. Finance records the resolution before returning to review. Duplicate clearance and rejection/reopening require reasons. Comments remain literal text, never trusted HTML; invalid commands must not partially change audit or version.

**Use and extend**: Use the explicit resolution action after reviewing the answer. For new exception types, define how readiness, signatures and version change; add the audit event and EN/DE error message.

**Backend tests**: `InvoiceModuleTest, DomainTest, ModuleIntegrationTest`.

**Frontend tests**: `workspace.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js`.

## 7. Approval policy and delegation

**Source**: `domain/WorkflowPolicy.java`; `domain/WorkflowFacts.java`; `domain/DelegationPolicy.java`; `identity/IdentityService.java`; `catalog/CatalogService.java`.

**API**: `/api/invoices/{id}/commands, /api/settings/rules, /api/delegations`.

**Data**: `invoice approval slots and threshold snapshot, delegation, organization`.

Submission requires valid basics, complete allocation, cleared duplicate concern, accepted required evidence and no open questions. Snapshot the high-value requirement at submission. The creator cannot approve. The assigned approver/delegate signs first; a different administrator signs second when needed. Delegation uses inclusive start and exclusive end and does not bypass independence.

**Use and extend**: Change thresholds only as Admin. A new policy needs independently defined expected decisions, boundary money/date cases and cross-user tests. A role/state matrix does not replace detailed fact combinations.

**Backend tests**: `DomainTest, PolicyMatrixChecks, InvoiceModuleTest, CatalogModuleTest, IdentityModuleTest, IntegrationTest`.

**Frontend tests**: `workspace.test.jsx; catalog-settings.test.jsx; dialogs.test.jsx; workflows.spec.js; modules.spec.js`.

## 8. Accounting handoff and idempotency

**Source**: `exports/ExportService.java`; `exports/ExportController.java`; `domain/SafeCsv.java`.

**API**: `/api/exports, /api/exports/{id}/download`.

**Data**: `accounting_export, export_invoice, original/evidence bytes, audit_event`.

Finance/Admin only; approved facts are rechecked under locks. Maximum 20 distinct invoices and a bounded source-byte total. Persist a fingerprint of IDs/versions with the idempotency key. Same key/payload returns the existing package; mismatched payload fails. CSV formula prefixes are neutralized and documents remain byte-identical. Package creation is not booking/payment or DATEV delivery.

**Use and extend**: Review and download the package from Accounting handoff. A live connector must have its own delivery status, credentials, retries, idempotency and real-provider tests; do not reuse EXPORTED as remote acceptance.

**Backend tests**: `ExportModuleTest, DomainTest, IntegrationTest, ModuleIntegrationTest, ConcurrencyIntegrationTest`.

**Frontend tests**: `accounting-audit.test.jsx; dialogs.test.jsx; modules.test.mjs; workflows.spec.js`.

## 9. Audit history

**Source**: `audit/AuditService.java`; `audit/AuditController.java`.

**API**: `/api/audit?before=EVENT_ID; invoice detail audit`.

**Data**: `audit_event, database immutability trigger`.

Append the actual actor and organization in the caller transaction. Use keyset pagination, not an unbounded event dump. No public update/delete API exists. Database triggers prevent ordinary mutation but do not defeat privileged administrators. Do not log passwords or original document contents.

**Use and extend**: Navigate from an event to its invoice and compare the transaction history. Add a stable event code and both translations with every new business mutation.

**Backend tests**: `AuditModuleTest, ModuleIntegrationTest, InvoiceModuleTest`.

**Frontend tests**: `accounting-audit.test.jsx; modules.test.mjs; modules.spec.js`.

## 10. Reminders and notifications

**Source**: `notifications/ReminderService.java`; `notifications/NotificationController.java`; `domain/ReminderPolicy.java`.

**API**: `/api/notifications, /api/notifications/{id}/read, /api/notifications/run`.

**Data**: `notification, invoice, delegation, active app_user`.

Use Europe/Berlin calendar dates, due horizon and active eligible recipients. Database uniqueness prevents duplicate reminders per recipient/invoice/day. Only the recipient may mark a notice read; only Admin starts a manual scan. Notifications are in-app. Scheduled initial delay is configurable and deliberately longer in tests.

**Use and extend**: Open the bell, follow the invoice, mark the chosen item read. Email delivery is a separate future adapter requiring actual transport, failure and privacy handling.

**Backend tests**: `NotificationModuleTest, DomainTest, ModuleIntegrationTest`.

**Frontend tests**: `catalog-settings.test.jsx; dialogs.test.jsx; modules.test.mjs; modules.spec.js`.

## 11. Bootstrap, SQL normalization and configuration

**Source**: `bootstrap/BootstrapData.java`; `shared/Sql.java`; `shared/ApiErrors.java`; `BelegFlowApplication.java`; `resources/application.yml`; `resources/db/migration/V1__initial_schema.sql`.

**API**: `Startup, health endpoint, all typed inputs/error responses`.

**Data**: `Flyway schema; initial organization/admin or explicit synthetic demo`.

Production bootstrap needs explicit secure credentials and must not silently create demo identities. Normalize numeric/date SQL values without floating-point loss. Unexpected errors expose a reference, not SQL/internal traces. Jackson 3 YAML must bind before application startup. Keep migrations immutable once deployed.

**Use and extend**: Change config with a real-context regression test, not only string matching. Add a new migration for schema changes. Test first startup, repeat startup and non-demo behavior independently.

**Backend tests**: `BootstrapModuleTest, SharedModuleTest, AdapterTest, JacksonConfigurationTest, ModuleIntegrationTest`.

**Frontend tests**: `http.test.mjs; shell-app.test.jsx; identity.test.jsx`.

## 12. Frontend layers and screen contracts

| File | Contract | Tests |
|---|---|---|
| `frontend/src/api.ts` | Same-origin HTTP, CSRF refresh, structured errors, binary download; no DemoApi fallback. | `http.test.mjs` |
| `frontend/src/store.ts` | Single state owner, request tickets, captured invoice/version, private-state clearing and explicit mutations. | `core.test.mjs, modules.test.mjs` |
| `frontend/src/format.ts + i18n.ts` | Exact cents, language-key parity, interpolation and separate number locale. | `core.test.mjs, modules.test.mjs` |
| `frontend/features/identity/Login.jsx` | Credentials, demo visibility, language selection, busy/error states. | `identity.test.jsx` |
| `frontend/features/invoices/Workspace.jsx` | Queue, invoice sheet, evidence, readiness, actions and discussions. | `workspace.test.jsx` |
| `frontend/features/catalog/CatalogPages.jsx` | Supplier/project/cost-center pages; business records are not auto-translated. | `catalog-settings.test.jsx` |
| `frontend/features/settings/Settings.jsx` | Admin controls, members/delegation, approval rules and personal preferences. | `catalog-settings.test.jsx` |
| `frontend/features/accounting/Exports.jsx + features/audit/AuditPage.jsx` | Authorized handoff, download, evidence trail, keyset paging and invoice links. | `accounting-audit.test.jsx` |
| `frontend/components/Dialogs.jsx` | Native modal forms, validation attributes, locale, captured transaction context. | `dialogs.test.jsx` |
| `frontend/App.jsx + components/Shell.jsx + components/ui.jsx` | Composition, route guards, loading/errors, accessibility names and shared controls. | `shell-app.test.jsx` |
| `frontend/main.jsx + styles.css` | React19 root, browser subscription, keyboard commands, responsive layout and focus. | `workflows.spec.js, modules.spec.js` |

React tests render actual React DOM components in jsdom with action spies. State/HTTP tests exercise the real classes separately. Browser journeys test their integration with the actual backend. The offline showcase is an additional, explicitly limited demonstration.

## 13. Module change checklist

Write the rule and permission contract; test a negative case first; update DTOs and validated inputs; add any required new migration; preserve versioning, transactions and audit; update EN/DE copy and keyboard behavior; extend unit, SQL and browser contracts as appropriate; update the test matrix, API reference and changelog; run complete verification and inspect its reports.

## Test infrastructure: multipart request builders (0.2.1)

`ModuleIntegrationTest.pdf(String)` returns `MockMultipartHttpServletRequestBuilder`. It intentionally omits dates so missing-date integration cases remain meaningful. Callers chain `.param(...)` and security postprocessors without casts. `MultipartRequestBuilderTest` executes the helper with mocked SQL lookup results but real Spring request builders; its six cases verify request shape, exact uploaded bytes and fluent type safety. They do not claim database coverage. See [troubleshooting](TROUBLESHOOTING.md) and [execution evidence](../testing/VERIFICATION.md).
