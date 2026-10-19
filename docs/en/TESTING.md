# Testing, coverage and release verification

Version 0.2.2 | [Deutsch](../de/TESTING.md) | [Module reference](MODULE_REFERENCE.md) | [Actual execution evidence](../testing/VERIFICATION.md)

## What "all modules" means

Every current business module has named tests: identity/security, supplier and project catalogs, invoice review/allocation, documents/imports, evidence, clarification/duplicates, approvals/delegation, accounting exports, audit, reminders, bootstrap and shared errors. Frontend tests cover every feature page, transactional dialog, shell, shared control, HTTP client, store, formatting and translation dictionary. Configuration is executable behavior and has its own regression suite.

A case inventory is not a measured coverage percentage. A mocked service test does not execute PostgreSQL. A React/jsdom test does not prove browser layout or native validation. A source parser does not prove Java compilation. A compatibility showcase is not a production deployment. Read the layer-specific results rather than adding all numbers together and calling the release "fully passed".

## Recommended complete run: Docker

Use Docker Engine/Desktop with Compose and network access to the configured registries. No host Maven, Java or Node installation is required for this route. Run from the repository root.

```powershell
# Windows PowerShell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-all.ps1
```

```bash
# Linux / macOS / WSL. Stop only an older dedicated test stack first.
docker compose -f compose.quality.yml down
docker compose -f compose.quality.yml up --build --abort-on-container-exit --exit-code-from verifier
# After reading the exit status:
docker compose -f compose.quality.yml down
```

The `belegflow-quality` project uses database `belegflow_test` on an ephemeral PostgreSQL filesystem. It does not use the application's `pgdata` volume and does not expose a database port. Its password is a public local-test value, not a production secret. The helper explicitly requires `BF_TEST_DATABASE_ACK=DISPOSABLE` and rejects other database names. The nontransactional concurrency cases leave synthetic rows until this test instance is removed.

The run installs dependencies, type-checks, runs core/React tests with V8 coverage, builds the production React client, runs all Maven/JUnit/PostgreSQL cases with JaCoCo, checks that the three database suites actually ran, starts the built Spring application, and runs the 25 real-application browser journeys. An error stops the run. Reports are collected even on failure; a failed build is never replaced with the offline preview.

## Commands by layer

| Layer | Command from repository root | Scope |
|---|---|---|
| Domain policies without downloads | `sh scripts/test-domain-offline.sh` | 306 deterministic cases, JDK 21+ compatible helper. |
| Configuration regression | `./mvnw -Dtest=JacksonConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | Actual YAML binding, ISO dates/instants and the old failure. |
| Backend unit/configuration | `./mvnw test` | Domain and server unit cases; database cases require the explicit integration flag. |
| Frontend core | `cd frontend && npm run test:core` | 100 HTTP/store/format/i18n cases. |
| React components | `cd frontend && npm run test:components` | 140 expanded cases using the actual React dependency and jsdom. |
| Frontend coverage | `cd frontend && npm run test:coverage` | Both Node and DOM Vitest projects; includes unimported production source. |
| Browser journeys | `cd frontend && npm run test:e2e` | 25 specifications against the running real application, not DemoApi. |
| Complete verification | `sh scripts/verify-all.sh` | All layers; requires configured tools and disposable PostgreSQL. |

Do not run multiple full-stack jobs against the same test database. Test order must not be used as a business prerequisite. Database transactional cases roll back; concurrency cases use independent organization IDs. E2E forms create uniquely named synthetic records. Scheduled reminders are delayed during testing so explicit reminder cases control their timing.

For manual full-stack verification, set `BF_DATABASE_URL=jdbc:postgresql://localhost:5432/belegflow_test`, matching user/password, `BF_INTEGRATION=true` and `BF_TEST_DATABASE_ACK=DISPOSABLE`. Install the configured Java/Node releases and Chromium with `npx playwright install --with-deps chromium`. The Docker route configures these automatically.

## Coverage and reports

JaCoCo reports live under `domain/target/site/jacoco/` and `server/target/site/jacoco/`. V8 reports live under `frontend/coverage/`. The frontend exclusions are type-only `models.ts` and the production bootstrap `main.jsx`, which the actual browser journeys exercise. No business class is excluded simply because coverage is low. View JSX is executed in React tests but is not part of the strict TypeScript compilation surface.

Enable the explicit release gate with the PowerShell `-StrictCoverage` switch or `BF_STRICT_COVERAGE=true` in the quality stack. Backend targets are at least 80% line and 70% branch per Maven bundle. Frontend targets are 80% statements/functions/lines and 70% branches across included source. These are configured targets, not measured achievements. Module-by-module review remains necessary because aggregate percentages can conceal weak areas.

`quality-results/` contains JUnit XML, JaCoCo/V8 HTML and machine-readable reports, Playwright traces/screenshots on failure, server logs, a generated dependency lockfile when available, and `exit-code.txt`. Open HTML reports locally. Use `npx playwright show-trace PATH` for a failed journey. Never commit real invoice data captured in logs/screenshots.

`scripts/assert-integration-reports.py` requires all three suites: `IntegrationTest` (15), `ModuleIntegrationTest` (29), and `ConcurrencyIntegrationTest` (3). Missing reports, skipped cases, failures or errors reject the run. It must not be removed to make a release green.

## GitHub Actions

The workflow has three jobs: an explicit backend test-compilation step and database-free Jackson/multipart regressions, complete frontend checks/build, and the dependent full-stack job. Each publishes evidence. The frontend job also runs `npm audit --audit-level=high`; this additional vulnerability gate may fail even when functional tests pass. Workflow dispatch can request strict coverage. A successful default run measures coverage but does not certify that the optional release threshold or manual security/accessibility gates were approved.

The ZIP deliberately contains no invented dependency lockfile. A networked first resolution generates it. Review and commit it, rerun using `npm ci`, then archive the commit SHA and CI artifacts used for release. A badge should link to the real workflow outcome, not a static green image.

## Writing a new test

Start with the violated rule and the smallest failing test. Put deterministic calculations/transitions in `domain`, adapter mapping/permissions in a server module test, SQL/transaction behavior in `ModuleIntegrationTest`, and a race in `ConcurrencyIntegrationTest`. In the frontend, test core state through `FixtureApi`, the rendered screen through React Testing Library, and a critical complete journey through Playwright. Add EN and DE assertions when visible copy changes.

Use independent fixtures, exact decimal strings, fixed clocks or controlled dates, and synthetic IDs. Assert both the intended result and the absence of a forbidden side effect. Avoid sleeps to hide races, testing implementation text alone, copying the algorithm under test into its expected values, or mocking the service that the test claims to verify. A regression for a bug must fail before the fix and pass afterwards when run in the supported environment.

## Remaining manual gates

Full automated success is necessary, not sufficient: keyboard/screen-reader review; accessibility audit; threat-model review; dependency/license/security triage; backup restoration; production TLS/secrets; actual accountant handoff review; and customer-specific retention/privacy requirements. Neither coverage nor a business approval certifies legal compliance.


## Report gate self-tests

`python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v` runs nine isolated tests of the evidence gate. Their XML is intentionally synthetic and must never be copied into application Surefire reports. The full verification runs these before collecting application evidence.

## 0.2.1 multipart regression / Multipart-Regression

```bash
# Focused check, JDK 25 and access to Maven repositories required:
./mvnw --batch-mode --no-transfer-progress test-compile
./mvnw --batch-mode --no-transfer-progress -Dtest=JacksonConfigurationTest,MultipartRequestBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

`MultipartRequestBuilderTest` uses the actual test helper with mocked SQL lookup values. It does not connect to PostgreSQL and does not replace the import integration tests. Its six cases passed in the supplied 0.2.1 CI run; the full 0.2.2 release still needs its own gate. See [the verification boundary](../testing/VERIFICATION.md).


## 0.2.2 browser regressions

Current declared totals are 100 client-core, 140 React component and 25 real-server E2E cases. New real-server cases cover five viewport shapes in EN/DE and a deliberately delayed logout request that still reaches the actual backend. The administration helper awaits the successful logout response and verifies a 401 session before the next login. Threshold cleanup is in `finally`. Exact labels are not replaced with loose text matching.

The uploaded 0.2.1 run now proves backend test compilation, all 491 backend cases, and 224 frontend cases passed; only two browser cases failed. The new package has local scoped results, not a completed production rerun. See the dated [verification report](../testing/VERIFICATION.md). For the optional standalone-only regression script use `python scripts/test-ui-regressions.py` with Python Playwright and Chromium. Its 13 cases and the 17 existing showcase checks must never be presented as real-server E2E results.

Documentation-only browser QA: `python scripts/test-handbook.py` checks seven EN/DE navigation, search, evidence and mobile-layout scenarios. It requires Python Playwright and Chromium and does not count toward application E2E coverage.
