# Changelog

## 0.2.0 - 2026-10-18 - test and documentation expansion

### Fixed

* Replaced the incompatible Jackson 2 date property with `spring.jackson.datatype.datetime.write-dates-as-timestamps: false` for Jackson 3. The uploaded run failed to create the application context; its 15 integration errors shared this cause.
* Added database-free tests that load the actual YAML, assert ISO date/instant output, and reproduce the legacy property's failure. The remedy does not downgrade Jackson or disable integration tests.
* PDF imports with missing or malformed dates now raise the structured `INVALID_INPUT` validation error instead of an unexpected server error.
* Cleared private client state on authentication changes and strengthened stale-response suppression around sign-in, sign-out, navigation, audit and mutation requests.
* Clear cached CSRF state on HTTP 401; retain distinct authentication, authorization, missing-file and server-error download messages.
* Made reminder initial delay configurable (30 seconds by default); isolated full-stack tests delay scheduled runs and invoke reminder logic explicitly.

* Corrected the landing page language-control function name to avoid an HTML `lang` property collision; documentation navigation and search were browser-checked.

### Added

* 224 role/state/action matrix cases alongside the original 82 domain cases.
* 132 server unit/configuration cases across all server packages (includes the existing 12 adapter cases), plus 47 real-PostgreSQL integration cases.
* 92 frontend core cases, 132 React/DOM cases and 14 real-application Playwright journeys. Counts describe declared test cases; see the verification report for executed results.
* JaCoCo and V8 coverage reporting, JUnit XML, an explicit strict-coverage mode, and a check that rejects missing/skipped database integration reports.
* A dedicated disposable PostgreSQL quality stack and Windows/POSIX verification commands.
* Module-by-module EN/DE references, testing and troubleshooting guides, a test inventory, updated code/API guides and a searchable bilingual HTML handbook.

### Compatibility and limits

No database migration was changed or added. Existing data volumes should be preserved. This is an engineering candidate; the complete networked build and new dependency graph were not executed in the authoring environment. No live DATEV, invoice-standard certification, SSO, billing or email feature is introduced. Review and commit the generated dependency lockfile before a commercial release.
