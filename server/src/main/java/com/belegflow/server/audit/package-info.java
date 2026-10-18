/**
 * Append-only business history. Events are written in the caller transaction and read through tenant-scoped cursor pagination. Privileged database operators are outside the immutability guarantee.
 * <p>See docs/en/MODULE_REFERENCE.md and docs/de/MODULE_REFERENCE.md for use, tests and extension contracts.</p>
 */
package com.belegflow.server.audit;
