# BelegFlow 0.2.1 verification report

Release date: October 18, 2026. Status: **targeted compiler-fix engineering candidate**.

[Observed CI evidence](../CI-106020289651.md) | [Module matrix](../TEST_MATRIX.md) | [English troubleshooting](../../en/TROUBLESHOOTING.md) | [Deutsche Fehlerdiagnose](../../de/TROUBLESHOOTING.md)

## What was fixed

`ModuleIntegrationTest.pdf(String)` declared `MockHttpServletRequestBuilder` while returning the separate, self-typed `MockMultipartHttpServletRequestBuilder`. The return declaration now matches the multipart value. No unsafe cast, library downgrade, disabled test, or application behavior change is used to hide the failure.

Six new database-free `MultipartRequestBuilderTest` cases use the actual integration helper and Spring request builders. SQL lookup results alone are mocked. They check the return contract, original PDF bytes, metadata/decimal fields, deliberately absent dates, date-parameter chaining, and request-postprocessor chaining.

The CI preflight now exposes `test-compile` as its own step, then runs both Jackson and multipart regression suites. The PostgreSQL/real-browser gate remains in place. Dependencies have not been upgraded or downgraded in this repair.

## Three distinct kinds of evidence

### 1. Observed in the user's supplied 0.2.0 CI logs

* Frontend: **224 real Vitest tests passed** across 9 files (92 core + 132 component).
* TypeScript check and Vite production build passed.
* Aggregate coverage: statements **86.49%**, branches **84.82%**, functions **88.44%**, lines **87.38%**.
* npm audit reported 0 vulnerabilities in that run.
* Backend production source compilation completed, but test compilation failed. The Jackson tests and full-stack verification were not successfully executed.

These observations replace the former uncertainty about whether the supplied React component suite could run at all. They are not results of a 0.2.1 rerun. Frontend source, tests, tooling configuration, and dependency selections are unchanged by this patch; only the package's release version changes.

### 2. Executed locally again for 0.2.1

| Check | Result | Scope |
|---|---|---|
| Java domain policy/parser/file checks | 306/306 passed | JDK 21-compatible offline harness; not the Spring/JDK 25 build |
| Client core tests | 92/92 passed | Local Node 22 strip-types harness; not the pinned Node 24 production toolchain |
| Report-gate self-tests | 9/9 passed | Synthetic report fixtures only, never application results |
| Documentation browser checks | 7/7 passed | System Chromium; English/German routing, search, mobile overflow and landing-page controls only |
| Local TypeScript check | Passed | Installed compiler; not a substitute for the pinned production toolchain |

The archived local logs are `domain-local.log`, `frontend-local.log`, and `quality-gate-local.log`; documentation-only browser results are in `documentation-browser-0.2.1.json`. Final package checks are recorded in `package-validation.json` after packaging. No results are invented for unavailable dependencies.

### 3. Still requires the real networked environment

| Suite / gate | Declared cases or requirement | Current status |
|---|---:|---|
| Server unit/configuration suite | 138 (132 existing + 6 multipart regressions) | Not executed here |
| PostgreSQL integration | 47 | Not executed here |
| Real React component suite on 0.2.1 | 132 | Not rerun here; passed for unchanged component source in supplied 0.2.0 logs |
| Real-application Playwright | 14 | Not executed here |
| Complete Spring/JDK 25 build | Maven + pinned dependencies | Not executed here |
| Backend measured coverage | JaCoCo | Unknown until full-stack execution |

This authoring environment has no Maven, Docker, or PostgreSQL, has JDK 21 instead of JDK 25, and cannot resolve the Maven/npm registries. A source review and the official API hierarchy support the correction, but are not a compiler or runtime test. **The six new Spring regressions and the complete corrected build must pass CI before release.**

The earlier 17 compatibility-browser checks are historical evidence for the standalone showcase; they have not been rerun for this repair and are not real-server tests. Their historical JSON artifacts remain labelled as compatibility evidence.

## Reproduce the focused check

From the repository root, with JDK 25 and registry access:

```bash
./mvnw --batch-mode --no-transfer-progress test-compile
./mvnw --batch-mode --no-transfer-progress -Dtest=JacksonConfigurationTest,MultipartRequestBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The selector's `surefire.failIfNoSpecifiedTests=false` permits the domain module (which has neither selected class) to participate in the reactor; it does not suppress failed tests or compilation errors. Do not use `maven.test.skip=true` as a workaround.

For all layers, use the documented `compose.quality.yml` or `scripts/verify-all.ps1` route with a disposable database, then inspect the uploaded `full-stack-verification` artifacts.

## Data and release safety

No Flyway migration, invoice business rule, or authorization rule changed. Preserve the existing database volume and deployment secrets. Keep intentional repository changes and any existing reviewed `frontend/package-lock.json`; its top-level package version should be updated to 0.2.1 without unnecessarily re-resolving dependencies. Re-run with `npm ci` after committing the lockfile.

Manual security/accessibility/privacy review, TLS/secrets configuration, restore verification, accountant handoff review, and dependency/license review remain release requirements. No DATEV integration or legal certification is claimed.

## Zusammenfassung auf Deutsch

Der neue Fehler liegt im Testcode: `pdf(String)` muss `MockMultipartHttpServletRequestBuilder` zurückgeben. Der Jackson-Test wurde im hochgeladenen Lauf wegen des vorherigen Kompilierungsfehlers nicht ausgeführt. Der Frontend-Lauf war erfolgreich: 224 Tests, TypeScript-Prüfung und Produktions-Build; die gemessene Zeilen-Coverage beträgt 87,38 %. Diese Werte stammen aus dem bereitgestellten 0.2.0-Protokoll, nicht aus einem vollständigen 0.2.1-Lauf.

Lokal wurden für 0.2.1 erneut 306 Domänenfälle, 92 Client-Kerntests und 9 Berichtskontrollen ausgeführt. Die sechs neuen Multipart-Tests sowie der vollständige Spring-/PostgreSQL-/Browser-Lauf bleiben bis zur CI-Ausführung offen. Es ist keine Datenbankmigration und kein Zurücksetzen der Daten erforderlich.
