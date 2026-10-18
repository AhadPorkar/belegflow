# BelegFlow 0.2.0 verification report

Date: October 18, 2026. Status: engineering candidate with expanded tests; full-stack execution pending.

## Actually executed for this revision

| Layer | Result | Evidence and limit |
|---|---|---|
| Java domain rules and role/state/action matrix | **306 / 306 passed** | `domain-local.log`; actual source compiled and executed with OpenJDK 21.0.12.1 using the offline helper. Not a Maven/Java 25 run. |
| Frontend core tests | **92 / 92 passed** | `frontend-local.log`; real store, HTTP, formatting/i18n code, Node 22.16.0. |
| Report-gate unit tests | **9 / 9 passed** | `quality-gate-local.log`; synthetic XML verifies that missing/skipped/failed integration evidence is rejected. These fixtures are not app results. |
| Strict TypeScript check | **Passed** | Local TypeScript 5.8.3, `tsc --noEmit`; typed `.ts` application files, not JSX views. |
| Compatibility browser checks | **17 / 17 passed** | `browser-compatibility.json`; Chromium 144.0.7559.96, bundled React 16 and browser-local synthetic adapter. Not React 19/Spring/PostgreSQL proof. |
| Showcase accounting ZIP and original hash | **Passed within browser checks** | Archive integrity and synthetic original SHA-256 were checked. Not an execution of the Spring export service. |
| Documentation browser checks | **Passed** | `documentation-browser.json`; 12 guide views in each language, search, mobile layout and landing-page language controls. |
| Static checks | **Passed** | Java parse-only, frontend syntax parsing, JSON/XML/YAML parsing, shell syntax and local documentation links. Syntax parsing is not dependency resolution or compilation of Spring tests. |

The core domain result comprises the original 82 cases plus an independently defined 224-case role/state/action matrix. Node and Vitest reuse the same frontend core case bodies; do not count them as twice as many distinct specifications. Browser checks include real rendered dialogs and some store-driven navigation, German layout, dark mode and mobile panes. No uncaught compatibility page errors occurred.

## Supplied but not executed in the supported full stack here

| Suite | Declared cases | Required environment |
|---|---:|---|
| Server unit and configuration tests | 132 | Maven, Java 25, Spring/JUnit/Mockito dependencies; four load real Jackson configuration without a database. |
| Real PostgreSQL integration | 47 | Three suites: original 15, module contracts 29, independent-connection concurrency 3. |
| React component tests | 132 | Configured React 19.3.0, Vitest/jsdom/Testing Library dependencies. |
| Real application browser journeys | 14 | Built React/Spring application and disposable PostgreSQL, actual Playwright Chromium. |

The 306 domain cases also have a JUnit factory, but that factory was not executed locally. The complete Java 25 / Spring Boot 4.1.1 / PostgreSQL 18.6 / TypeScript 7.0.2 / React 19.3.0 / Vite 8.3.4 build is not newly verified by this report. Docker startup, fresh migrations, real coverage percentages, penetration/accessibility audits, production restoration and load/soak testing are not claimed as executed.

The authoring container has no Maven, Docker, PostgreSQL or downloadable npm/Maven dependency graph. Networked tools therefore must run the supplied full verification before a customer release. A version source page is not a build result. No lockfile, SBOM, image digest or green CI badge was invented. The added server/test Java was syntax-parsed, not type-checked against unavailable libraries.

## Root cause fixed from the uploaded run

The provided log `full-stack/8_Spring, domain and real PostgreSQL integration tests.txt`, line 1347, reports:

```text
No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

The old Jackson 2 serialization property prevented Spring's context from starting. Its 15 integration errors shared this cause. The revised YAML configures `spring.jackson.datatype.datetime.write-dates-as-timestamps: false`; the old property must also be removed from environment overrides. `JacksonConfigurationTest` loads the actual application YAML, tests ISO date/instant output and intentionally reproduces the old failure.

Separate historical evidence from the uploaded v0.1.0 run: 82 domain cases passed; 12 server adapter cases passed; 24 frontend core cases and the production frontend build passed; 15 database integration methods errored during context startup. These are not pass results for newly changed source.

## Reproduce all layers and collect evidence

Windows, from the revised repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-all.ps1
```

Linux/macOS/WSL with Docker:

```bash
docker compose -f compose.quality.yml down
docker compose -f compose.quality.yml up --build --abort-on-container-exit --exit-code-from verifier
```

Use only the dedicated disposable quality stack. It has no application-data volume. Inspect `quality-results/` and the process exit status before removing the test stack. Strict coverage: PowerShell `-StrictCoverage`, or `BF_STRICT_COVERAGE=true` in the quality environment. Targets are 80% lines and 70% branches (frontend also 80% functions/statements), not measured results. A plain `mvn verify` may skip database tests; the full verification command rejects that condition.

The CI has a first database-free configuration-regression job, a frontend/coverage/build job, and a dependent real full-stack job. Review and commit its generated lockfile, rerun from that commit and retain the reports. Do not display a passing badge or describe the package as production-certified before actual evidence supports it.

## Coverage of module contracts

See [test matrix](../TEST_MATRIX.md) and [machine-readable case inventory](../test-inventory.json). Every implemented backend package and frontend feature has named tests, including negative authorization, invalid input, stale response/version, independent approval and export retry paths. This is broad module coverage by test design, not proof that every branch or possible input has been exercised. A passing automated suite does not replace a security, accessibility, accounting or legal review.

## Deutsch: Ergebnis und Grenze

Tatsächlich lokal erfolgreich: 306 Java-Domänenfälle, 92 Client-Core-Fälle, 9 Tests der Berichtskontrolle, strikte lokale TypeScript-Prüfung und 17 begrenzte Browser-Kompatibilitätsprüfungen. Die Vorschau verwendet React 16 und einen simulierten Adapter; sie beweist keinen Produktivbetrieb mit React 19, Spring oder PostgreSQL.

132 Server-Unit-/Konfigurationsfälle, 47 echte Datenbankfälle, 132 React-Komponentenfälle und 14 echte Browser-Journeys sind enthalten, hier aber nicht mit ihren vollständigen Abhängigkeiten ausgeführt. Maven, Docker, PostgreSQL und Registry-Zugriff fehlen in der Erstellungsumgebung. Keine bestandene CI, Coverage-Prozentwerte oder Lockdatei werden erfunden.

Der hochgeladene Altfehler stammt aus einer Jackson-2-Konfiguration in Jackson 3. Die korrigierte YAML-Datei und datenbankfreien Regressionstests sind enthalten. Das Upgrade verlangt keine geänderte Migration und keinen Datenreset. Vor Freigabe den isolierten Gesamtlauf ausführen, die Lockdatei prüfen/committen und Berichte sowie manuelle Sicherheits-/Betriebsprüfungen bewerten. Anleitung: [Tests auf Deutsch](../../de/TESTING.md).
