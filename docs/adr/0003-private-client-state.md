# ADR 0003 - Scope browser state to an authenticated identity

Status: implemented in 0.2.0. Late requests may complete after logout, navigation or a different login. Decision: clear private data on identity changes and use request tickets to discard stale responses. Capture invoice ID/version and export key in transaction context. Backend authorization remains authoritative; client cleanup is defense in depth, not tenant security by itself. Regression cases cover late responses, session expiry, auth transitions and safe export retries.
