# BelegFlow user guide

Version 0.1.0 | [Deutsch](../de/USER_GUIDE.md)

## 1. Understand the three decisions

A basic document check asks whether supported invoice fields can be read and whether the supported arithmetic/date checks pass. It does not certify the invoice under a German e-invoicing standard. Business approval asks whether an independent responsible person accepts the expense, allocation and required service evidence. Accounting handoff packages approved information for a separate accounting process. An exported invoice is not automatically booked or paid.

The normal path is `Received -> Reviewed -> Awaiting approval -> Approved -> Exported`. A clarification detour returns the invoice to Reviewed after its open questions are resolved. Rejection requires a reason; Finance may reopen a rejected record. Reopening invalidates earlier approvals so the business decision can be repeated.

## 2. Sign in and choose preferences

Use the account provided by your administrator. Passwords are case-sensitive. Sessions expire after 30 minutes of inactivity. Administrative changes to an account revoke its existing session through an authentication-version check. Sign in again when a session is revoked; do not keep retrying an old form.

The initial interface language is English even in a German browser. Choose EN/DE in the header. A signed-in user's choice is saved. In Workspace settings, choose a separate number/date preference: English-style `1,234.56` or German-style `1.234,56`. Money remains EUR; changing display formatting does not convert currency or alter stored values. Timestamps are displayed for Europe/Berlin. Light/dark preference is local to the browser.

The role selector and one-click account buttons appear only in the explicitly enabled demo. They are not production authorization controls.

## 3. Roles

| Role | Main responsibilities |
|---|---|
| Finance | Import, review, allocate, resolve exceptions, submit and export; manage suppliers/projects/cost centers. |
| Approver | Accept project evidence when responsible, discuss assigned work, approve or reject assigned invoices. |
| Administrator | Finance functions, user/rule/delegation management and independent second approval. |
| Viewer | Read-only access to the organization's information; cannot change decisions or create exports. |

An administrator does not automatically bypass first-approver assignment or self-approval prevention. The creator cannot approve their own invoice. The first approval belongs to the assigned approver or a currently active delegate. Where two approvals are required, the second signer must be a different administrator and must also differ from the creator.

All active organization members can read their organization's invoices and download their documents. Assignment governs action rights, not record visibility. Organizations are isolated from each other; there is no shared multi-organization user membership in this release.

## 4. Use the workspace

The left pane contains search, status filters and a paged list. The center shows either a structured invoice representation or service evidence. The right pane shows readiness, decisions, activity and discussions. The invoice illustration is generated from extracted or manually entered fields; it is **not a rendering of the original PDF/XML**. Use Download original to inspect the preserved original.

On a phone, switch between Queue, Document and Decision instead of compressing all three panes. Use the header search/command control, or Ctrl/Cmd+K on a keyboard, to navigate. Only controls available to the current role and state should appear. A disabled submission button is accompanied by the missing readiness conditions.

## 5. Import an invoice

Choose Import invoice and select one file. Originals are limited to 10 MB. The server accepts a supported XML or a file with a PDF signature; it does not perform a full PDF semantic or malware inspection.

For XML, the server supports bounded UBL Invoice and CII header extraction: supplier name, invoice number, issue/due dates, currency, description and monetary totals. It checks supported totals, EUR, dates and required fields. This is not a general-purpose XML converter or official Schematron validator. The provided XML samples are extraction fixtures, not certified XRechnung examples. Credit notes, partial/prepayment semantics and embedded ZUGFeRD extraction are outside scope.

For PDF, choose a supplier and enter invoice number, description, dates, net, tax and gross. There is no OCR. Enter decimals using the numeric form control; the API uses decimal strings with a point, not floating-point financial calculations. Line items, tax-account determination and VAT-number verification are not implemented.

Optional project/cost-center/approver selections can be supplied during import or completed later. XML supplier names can match an active existing supplier or create a new supplier record. Review that match, especially where two legal entities have similar names.

An identical original hash in the same organization reopens the existing invoice rather than creating another. A different file with the same normalized supplier/invoice-number business key produces a warning that requires human investigation.

## 6. Review and allocation

As Finance, select a Received invoice and mark it Reviewed after checking the original and supported fields. Use Edit allocation to set its project, cost center, approver and due date. The project determines whether service evidence is required and who may accept it. Missing allocations or invalid basic checks block submission.

Changing a project's assignment while the invoice is editable resets its service acceptance. Adding another evidence file also resets acceptance: the reviewer must review the evidence set that is actually being approved. Metadata changes are not allowed after submission/approval through ordinary editing controls. The original bytes are never rewritten by allocation changes.

Supported checks are deliberately separate from visual presentation. When arithmetic or document semantics are unsupported, obtain a corrected original or resolve the issue outside the application. There is no unrestricted 'force valid' button or financial-field editor that silently changes a received XML invoice.

## 7. Service evidence

Use Service evidence to download existing evidence and attach a service report, handover note or other completion record. Supported attachments are PDF, PNG, JPEG and bounded plain text; at most 20 evidence files per invoice. Each file has a 10 MB limit, while text is limited to 128 KB. Keep sensitive unrelated information out of attachments.

When required, the project manager or valid delegate reviews the actual attachment and chooses Accept service delivery, with an explanatory note. The invoice creator cannot accept their own required evidence. A note without an attachment is insufficient. Acceptance is a business confirmation, not a qualified electronic signature or statutory inspection certificate.

After acceptance, Finance can submit only when basic validation, allocation, duplicate review and question resolution also pass. Evidence acceptance is not itself invoice approval.

## 8. Duplicates and clarification

Inspect a possible duplicate against the original and existing records. A corrected document is not automatically a separate payable expense. Finance may clear the warning only with a meaningful recorded reason. This does not delete the related document or guarantee that no duplicate payment will occur in a separate accounting system.

Choose Ask a question to move an editable/awaiting invoice into clarification and create an open question. Comments preserve context but do not automatically resolve questions. Finance records a resolution for each question, then returns the invoice to review. Submission is blocked while any question remains open. Earlier signatures are reset when a previously submitted invoice enters clarification.

Rejection requires a reason. A rejected invoice remains visible. Finance can reopen it with a reason and repeat review and approval; export cannot be used to bypass this state machine.

## 9. One or two approvals

Submission snapshots whether the invoice needs a second approval, using the organization's current gross-amount threshold. The default is EUR 10,000, inclusive. Later threshold changes do not silently remove the existing invoice's second-approval requirement.

For a normal invoice, the assigned independent approver completes the decision. For a high-value invoice, the first signature leaves it Awaiting approval; a different administrator completes the second. The same person cannot provide both signatures. Finance cannot approve merely because it imported the invoice. A user who is both administrator and creator still cannot self-approve.

A stale browser edit returns a version conflict rather than overwriting another person's decision. Refresh, inspect the updated history and make a new decision. Do not blindly resubmit an obsolete action.

## 10. Delegation and reminders

An administrator defines owner, delegate, start and end instants. The start is inclusive and the end exclusive. Delegation must be active at the moment of the action; the person clicking the action is recorded. Delegation does not remove self-approval or distinct-second-approver rules.

Reminders are generated for awaiting-approval invoices due within the next three business-calendar dates, including overdue invoices. The date is calculated in Europe/Berlin; this is not a holiday-aware working-day scheduler. The scheduled job runs every 15 minutes by default and deduplicates notices for the same user/invoice/day. Notifications appear under the bell. No outgoing email or SMS is sent. Administrators can trigger a run manually.

## 11. Accounting handoff

Finance or an administrator can export Approved invoices individually or as a batch of up to 20, with a 50 MB source-document budget. Large files may require smaller batches. The ZIP is persisted together with its SHA-256 and invoice membership; a retry using the same idempotency key and payload returns the same stored package.

The server package contains `invoices.csv`, `audit.csv`, `manifest.csv`, English/German notices, `originals/` and `evidence/`. CSV uses UTF-8 with BOM, semicolons, ISO dates and decimal points; fields are quoted and spreadsheet-formula prefixes are neutralized. The manifest lists document/data hashes; it does not hash itself. Original files are byte-preserved. Downloading a previous package does not create a new approval/export event.

The CSV is a documented generic handoff, **not a DATEV import format**. Your accountant must agree field mapping, accounts, tax treatment and import controls. Exported means that the package was created, not that the accounting system received or accepted it. No payment instruction is issued.

## 12. Troubleshooting

| Symptom | Action |
|---|---|
| Cannot submit | Read each readiness item: basic fields, allocation, duplicate warning, evidence and open questions. |
| Cannot accept evidence | Check project manager/delegate, creator restriction, invoice state and attachment presence. |
| First approval did not finish | Check whether an independent administrator must provide a second approval. |
| Version conflict | Refresh, compare history and retry only after reviewing the latest state. |
| Invalid XML | Verify supported document family, safe XML and required header fields; official conformance validation is separate. |
| Import says reused | The original hash already exists in this organization; use the existing invoice. |
| Session revoked | Sign out/in; an account or permission change may have invalidated the session. |
| No reminder email | Reminders are in-app only in this release. |
| Unknown server error | Give the administrator the reference identifier and time, not the confidential invoice contents. |

Report suspicious cross-organization access immediately. Do not upload customer invoices to the public portfolio or standalone showcase.
