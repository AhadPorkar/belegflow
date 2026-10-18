# HTTP API reference

Base path `/api`. Same-origin cookie session. Get `/csrf`, preserve its session cookie, and send its token as `X-CSRF-TOKEN` on every unsafe request, including login/logout. Login is URL-encoded `username` and `password`; success is 204. A login may rotate the session/token; fetch a new token afterwards. There are no bearer tokens or tenant headers to choose your organization.

All UUID references are checked in the authenticated organization. Money is a decimal string, dates are `YYYY-MM-DD`, event timestamps are ISO instants. Most mutations use `{version}`; missing/stale versions must not be hidden by client retry. Void mutations return 200 with an empty body. Login/logout are Spring Security endpoints, not MVC controller methods.

## Example: submit after refreshing details

```http
GET /api/invoices/INVOICE_UUID
Cookie: JSESSIONID=...
```

```http
POST /api/invoices/INVOICE_UUID/commands
Content-Type: application/json
X-CSRF-TOKEN: TOKEN
Cookie: JSESSIONID=...

{"version":3,"action":"SUBMIT","note":""}
```

Do not send raw status writes. Commands: REVIEW, CLARIFY, RESOLVE, SUBMIT, APPROVE, REJECT, REOPEN. EXPORT is represented by the export-package endpoint. CLARIFY/REJECT/REOPEN need a meaningful note; APPROVE may include a note. State and actor restrictions are enforced by the backend, not inferred from visible buttons.

## Endpoint inventory

| Method | Path | Controller operation |
|---|---|---|
| GET | `/api/csrf` | Anonymous session-bound CSRF token. |
| POST | `/api/login` | URL-encoded login; requires CSRF. |
| POST | `/api/logout` | Invalidate session; requires CSRF. |
| GET | `/api/audit` | `AuditController.events` |
| GET | `/api/catalog` | `CatalogController.all` |
| POST | `/api/suppliers` | `CatalogController.supplier` |
| PUT | `/api/suppliers/{id}` | `CatalogController.supplier` |
| POST | `/api/projects` | `CatalogController.project` |
| POST | `/api/cost-centers` | `CatalogController.costCenter` |
| PUT | `/api/settings/rules` | `CatalogController.rules` |
| POST | `/api/delegations` | `CatalogController.delegation` |
| DELETE | `/api/delegations/{id}` | `CatalogController.revoke` |
| POST | `/api/users` | `CatalogController.user` |
| PUT | `/api/users/{id}` | `CatalogController.user` |
| POST | `/api/invoices/import` | `DocumentController.ingest` (multipart). |
| POST | `/api/invoices/{id}/evidence` | `DocumentController.evidence` (multipart). |
| GET | `/api/documents/{id}` | `DocumentController.download` |
| GET | `/api/exports` | `ExportController.list` |
| POST | `/api/exports` | `ExportController.create` |
| GET | `/api/exports/{id}/download` | `ExportController.download` |
| GET | `/api/public/config` | `SessionController.config` |
| GET | `/api/session` | `SessionController.session` |
| PUT | `/api/session/locale` | `SessionController.locale` |
| GET | `/api/invoices` | `InvoiceController.list` |
| GET | `/api/invoices/summary` | `InvoiceController.summary` |
| GET | `/api/invoices/{id}` | `InvoiceController.get` |
| POST | `/api/invoices/{id}/commands` | `InvoiceController.command` |
| PUT | `/api/invoices/{id}/allocation` | `InvoiceController.allocation` |
| POST | `/api/invoices/{id}/duplicate-resolution` | `InvoiceController.duplicate` |
| POST | `/api/invoices/{id}/evidence-acceptance` | `InvoiceController.evidence` |
| POST | `/api/invoices/{id}/comments` | `InvoiceController.comment` |
| POST | `/api/invoices/{id}/questions/{question}/resolution` | `InvoiceController.resolve` |
| GET | `/api/notifications` | `NotificationController.list` |
| POST | `/api/notifications/{id}/read` | `NotificationController.read` |
| POST | `/api/notifications/run` | `NotificationController.run` |

## Payloads and multipart data

`GET /invoices`: optional `q`, `stage`, zero-based `page` and `size`; default 50, maximum 100. Response `{items,total,page,size}`. Detail includes the invoice, documents without bytes, discussion, audit, duplicate information, `allowed_actions` and explicit evidence capabilities. Summary returns counts and decimal-string aggregates.

`POST /invoices/import`: multipart `file` plus optional `supplier_id`, `project_id`, `cost_center_id`, `approver_id`. PDF additionally needs `invoice_number`, `description`, `issue_date`, `due_date`, `net`, `tax`, `gross`, `currency=EUR` and a supplier. XML derives financial fields from the original. Response `{id,reused,duplicate_warning}`. MIME headers are not trusted as sufficient file validation. At most 10 MB per file.

`POST /invoices/{id}/evidence`: multipart `file` and expected `version`. PDF/PNG/JPEG/text only within documented limits. `POST /evidence-acceptance`: `{version,note}`. `POST /duplicate-resolution`: `{version,note}`. `PUT /allocation`: `{version,project_id,cost_center_id,approver_id,due_date}`. Evidence acceptance requires project responsibility, not merely a general administrator role.

`POST /comments` and `POST /questions/{question}/resolution`: `{body}`. General comments do not change the invoice version on the server; question resolution does because it changes readiness. Questions are created through CLARIFY. Comments are not arbitrary HTML and are rendered as text.

`POST /exports`: `{idempotency_key:UUID,invoices:[{id:UUID,version:integer}]}`. One to 20 distinct approved invoices, bounded bytes. Response `{id,...}`; use `GET /exports/{id}/download` for the archive. Preserve the same key/payload to retry a lost response; a changed payload with the same key returns 409. Stored export metadata lists package IDs, SHA-256, time, user and invoice count. This endpoint creates a local accounting package, not a remote integration call.

`GET /documents/{id}` and export download return forced attachment, content type and nosniff headers. Original download also includes `X-Content-SHA256`. Do not embed returned invoice HTML or treat a PDF/XML as trusted browser markup.

`GET /audit`: keyset cursor `before` (an event ID), bounded page of 100. `GET /notifications`: current user's entries. Mark-read is scoped to that user. The administrator-only run endpoint invokes in-app reminders, not email delivery.

### Catalog request records

- `CatalogService.SupplierBody(String name,String email,String vat_id,boolean active)`.
- `CatalogService.ProjectBody(String code,String name,UUID manager_id,boolean evidence_required)`.
- `CatalogService.CostCenterBody(String code,String name)`.
- `CatalogService.RuleBody(String threshold)`.
- `CatalogService.DelegationBody(UUID owner_id,UUID delegate_id,Instant valid_from,Instant valid_until)`.
- `CatalogService.UserBody(String email,String display_name,Role role,String password,boolean active)`.
- `DocumentService.Download(String filename,String contentType,String sha256,byte[] bytes)`.
- `InvoiceImportService.Result(UUID id,boolean reused,boolean duplicate_warning)`.
- `ExportService.Entry(UUID id,int version)`.
- `ExportService.Request(UUID idempotency_key,List<Entry> invoices)`.
- `SessionController.LocaleBody(String locale)`.
- `InvoiceService.Command(int version,WorkflowAction action,String note)`.
- `InvoiceService.Allocation(int version,UUID project_id,UUID cost_center_id,UUID approver_id,LocalDate due_date)`.
- `InvoiceService.Note(int version,String note)`.
- `InvoiceService.Comment(String body)`.


Supplier writes and project/cost-center creation require Finance/Admin. Rule, delegation and user management require Admin. Updating a user with a blank password preserves the existing password; a supplied new password has a 14-128 character bound. Initial bootstrap has its separate minimum. A user's email is globally unique in this release. The active administrator cannot deactivate/demote itself through the user API.

## Errors

Response body contains `code`; unexpected errors also contain `reference`. Relevant status families: 400 malformed input; 401 absent/revoked session; 403 authorization/CSRF/separation-of-duties denial; 404 not found in the current scope; 409 stale version, idempotency mismatch or integrity conflict; 413 oversized upload; 422 unsupported business state/validation. Business codes include EVIDENCE_REQUIRED, ALLOCATION_REQUIRED, DUPLICATE_UNRESOLVED, OPEN_QUESTIONS, VALIDATION_FAILED, NOTE_REQUIRED and EXPORT_REQUIRES_APPROVAL.

The code reference and controller/request records are authoritative for this version. There is no undocumented third-party API promise, swagger-generated conformance badge or fake DATEV route.
