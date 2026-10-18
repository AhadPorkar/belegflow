# Verification report

Date: 2026-10-18. Release: 0.1.0 engineering candidate.

## Actually executed in this environment

| Check | Result | Scope and evidence |
|---|---|---|
| Pure Java checks | **82 / 82 passed** | `domain-local.log`; JDK 21.0.11, `javac --release 21`, actual business policy/parser/file/CSV code. |
| Client unit tests | **24 / 24 passed** | `frontend-local.log`; Node 22.16.0 built-in test runner, actual typed store/API/format/i18n sources. |
| TypeScript check | **Passed** | Local TypeScript 5.8.3; strict `.ts` files, not JSX views. |
| Browser compatibility checks | **17 / 17 passed** | `browser-compatibility.json`; local Chromium, explicitly labelled offline adapter/React 16 runtime. |
| Downloaded showcase ZIP | **Passed within browser checks** | ZIP integrity and original SHA-256 matched the selected synthetic source. Not a Spring export test. |

The browser journey includes actual rendered dialogs for evidence acceptance and approval, export download, high-value second approval, clarification, UBL import/re-import, German layout, dark theme and mobile panes. It also navigates via the same exposed store used by the showcase; not every navigation step is a physical mouse click. No uncaught browser runtime errors occurred. Screenshots show the compatibility rendering, not an executed React 19 production build.

## Supplied but NOT executed here

12 server adapter JUnit unit tests; 15 real Spring/PostgreSQL integration test methods; 4 Playwright real-application specifications. The 82 pure-core cases also have a JUnit factory, but JUnit/Maven was not run locally. These are not additional passed results. The networked CI must compile and execute them, and failures must be fixed before release.

Not executed: Maven/Spring full build, real PostgreSQL migrations, Docker image build/Compose startup, Java 25 runtime, React 19.3/Vite 8.3 production build, TypeScript 7.0.2, live dependency/advisory resolution, penetration test, accessibility conformance audit, load/soak tests, production TLS setup or backup restoration. No percentage coverage claim is made.

The creation environment had no Maven, Docker or PostgreSQL and could not resolve external package registries. Registry version pages were researched separately; their existence is not build evidence. **No npm lockfile, transitive SBOM, image digest or successful CI badge was invented.** Exact top-level frontend versions and the Spring BOM are configured; resolve, review and freeze the rest in the release environment.

## Reproduction commands

```bash
sh scripts/test-domain-offline.sh
cd frontend
npm install
npm run typecheck
npm test
cd ..
BF_INTEGRATION=true ./mvnw verify
# Start the actual built app and test against it:
cd frontend
npm run test:e2e
```

Use a dedicated disposable test database and the connection environment variables documented in OPERATIONS.md. `./mvnw verify` without `BF_INTEGRATION=true` intentionally skips the integration class; such a build is not a database test. `scripts/check-release.sh` deliberately blocks when the reviewed npm lock or integration opt-in is absent.

## Module-to-test map

| Module | Unit/policy surface | Integration/browser surface |
|---|---|---|
| Invoice workflow | WorkflowPolicy, facts, version adapter | Evidence-gated flow, stale version, independent second signer. |
| Identity | Actor roles, missing/unauthenticated principal | Login, CSRF, read-only mutation, cross-organization access. |
| Catalog/settings | Role rejection, threshold validation, delegation policy | Catalog scoping and non-admin rule denial. |
| Documents/import | Safe XML, file bounds, hash/normalization, scoped lookup, role rejection | XXE import denial, exact re-import, download headers. |
| Evidence | Policy gate and role rules | Acceptance, no creator bypass, complete flow. |
| Audit | Tenant/actor propagation | Transactional workflow/export history. |
| Export | Safe CSV, fingerprints, role gate | Stored package, original-byte match, idempotency retry. |
| Reminders | Date/timezone and empty scheduled run | Database daily deduplication. |
| Client | Exact money, locale parity, stale request races, CSRF/errors | Actual application specs plus separate compatibility checks. |

## Deutsch

Tatsaechlich ausgefuehrt wurden 82 Java-Kernpruefungen, 24 Clienttests, die lokale TypeScript-Pruefung und 17 Kompatibilitaetspruefungen im Browser. Die Offline-Vorschau verwendet einen simulierten Adapter und React 16; sie beweist keinen Spring-/PostgreSQL-/React-19-Betrieb. Der Originalhash im heruntergeladenen Vorschau-ZIP stimmt mit dem synthetischen Ausgangsdokument ueberein.

Die 12 Adaptertests, 15 echten Datenbank-Integrationstests und 4 echten Playwright-Spezifikationen sind vorbereitet, hier aber nicht ausgefuehrt. Maven, Docker, PostgreSQL und Registry-Netzwerkzugriff fehlten. Keine Lockdatei, SBOM, Image-Digests, bestandene CI oder Zertifizierung wurden erfunden. Vor Kundendaten den echten Build, Sicherheit, Betrieb, Wiederherstellung und rechtliche Anforderungen pruefen.
