# BelegFlow: evidence-first invoice decisions

## Website summary

BelegFlow is an independently implemented invoice-review product concept for German service businesses. Its central question is not only 'Does the document add up?' but 'Has the responsible person confirmed that the service was delivered?'

The product separates technical checks, service evidence, independent approval and accounting handoff. A custom three-pane interface keeps the queue, original-document access and decision context together. The project demonstrates pure Java policy design, explicit transactional SQL, tenant boundaries, optimistic concurrency, byte-preserving files, idempotent exports and bilingual interaction design.

## Demonstration narrative

Show the first invoice blocked despite passing basic checks. Switch to the project owner, inspect and accept evidence, then return to Finance to submit. Approve independently, create the accounting ZIP and inspect its manifest. Finish with a stale-update test, cross-tenant access test and the distinction between tests actually run locally and the full CI gate.

## CV wording after personally reviewing the implementation

'Built an evidence-first invoice-approval application using Spring Boot, PostgreSQL and React, with explicit workflow policies, tenant-scoped persistence, optimistic concurrency, original-document hashing and idempotent accounting exports.'

'Implemented English/German workflows and a responsive review interface; supplied domain, adapter, PostgreSQL integration and browser tests with reproducible deployment and operating guides.'

Do not claim production adoption, revenue, compliance certification, performance gains or a passing full-stack CI run until there is evidence. The project remains a portfolio/pilot engineering candidate. Mention AI assistance when discussing the development process rather than implying every implementation decision was unaided.

## Suggested repository presentation

Repository name: `belegflow`. Pin the repository after the owner has reviewed the source and the real CI passes. Lead with the screenshot, three-minute scenario, architecture diagram and test evidence. Use topics such as java, spring-boot, postgresql, react, typescript, workflow and invoice-approval. Keep the standalone compatibility showcase separate from the actual application deployment. Replace public demo credentials with a deliberately isolated demonstration host, never a customer installation.
