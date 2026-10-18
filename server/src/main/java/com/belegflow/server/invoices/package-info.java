/**
 * Tenant-scoped invoice queries and transactional commands. Row locks plus expected versions protect decisions; deterministic domain policies enforce readiness and independent approvals.
 * <p>See docs/en/MODULE_REFERENCE.md and docs/de/MODULE_REFERENCE.md for use, tests and extension contracts.</p>
 */
package com.belegflow.server.invoices;
