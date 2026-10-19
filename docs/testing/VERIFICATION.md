# BelegFlow 0.2.2 verification report

Release date: October 18, 2026. Status: **targeted browser-fix engineering candidate**.

[Observed CI evidence](CI-106036450086.md) | [Module matrix](TEST_MATRIX.md) | [English troubleshooting](../en/TROUBLESHOOTING.md) | [Deutsche Fehlerdiagnose](../de/TROUBLESHOOTING.md)

## Fixes

The fixed sidebar is vertically scrollable and does not compress its children into unreachable content. Short desktop screens omit the decorative note. Tablet widths now expose the topbar sign-out control, covering the former gap between desktop and mobile breakpoints.

Settings use explicit label/control IDs. Option text is outside the label and helper text is connected by `aria-describedby`. Existing implicit-label callers remain supported. Exact EN/DE labels are tested rather than replaced by a loose CSS selector.

Logout clears private data immediately, keeps login disabled until termination finishes, suppresses duplicate termination and competing login calls, and clears cached CSRF state even on a failed logout response. A failure is shown and releases the controls; a failed network response is not claimed to prove server-side termination.

E2E sign-out waits for a real successful `/api/logout` response and verifies `/api/session` returns 401. The next login is submitted on the existing login screen, not through an eager reload. Rule tests restore the original threshold in `finally`. Eleven added real-server journeys exercise five viewport shapes in EN/DE and a delayed logout.

No test was skipped or weakened to conceal a failure. No forced click, arbitrary sleep, dependency downgrade, migration change, or overall test-timeout increase was introduced. Per-action and navigation bounds of 10/15 seconds improve failure diagnostics; retries remain zero.

## Evidence A: supplied real CI for 0.2.1

| Check | Result |
|---|---|
| Domain | 306 passed |
| Server unit/configuration | 138 passed |
| PostgreSQL integration | 47 passed |
| Backend total / Maven build | 491 passed / BUILD SUCCESS |
| Frontend unit/component tests | 224 passed (92 core + 132 components) |
| TypeScript and production frontend build | Passed |
| Aggregate frontend coverage | 87.38% lines, 84.82% branches, 88.44% functions, 86.49% statements |
| Real-browser E2E | 12 passed, 2 failed |

These are observed results for **0.2.1**, not a completed rerun of 0.2.2. Backend implementation and tests are unchanged in this patch; release metadata is updated. The uploaded log does not contain the backend percentage report or the failed screenshot/trace contents. The administration cause is locally reproduced and consistent with the timeout, not independently established from its missing trace.

## Evidence B: executed locally for 0.2.2

| Check | Result | Scope |
|---|---|---|
| Domain checks | 306/306 passed | Offline JDK 21-compatible harness, not the JDK 25 Spring build |
| Frontend core | 100/100 passed | Node 22 strip-types harness, not pinned Node 24 |
| Evidence/report-gate tests | 9/9 passed | Synthetic test-report fixtures only |
| Targeted browser regressions | 13/13 passed | Real Chromium DOM interactions, React 16 compatibility renderer and synthetic DemoApi |
| Existing compatibility journeys | 17/17 passed | Standalone workflow/navigation/download scenarios; not production E2E |
| Local TypeScript and JS/JSX syntax checks | Passed | Installed compiler and Node; JSX transpilation is not a production build |

The thirteen targeted scenarios include ten viewport/language combinations plus member creation/relogin, independent persisted preferences, and controlled delayed logout. The original label lookup and offscreen sign-out defect were reproduced before patching. `scripts/test-ui-regressions.py` and its JSON report make this local scope explicit. The prior standalone browser result file is refreshed only after actually rerunning its script.

Documentation browser checks and final archive verification are recorded separately in `documentation-browser-0.2.2.json` and `package-validation.json`. Local logs are included; no synthetic XML is placed into real application test reports.

## Evidence C: next required networked run

Declared suites now contain **306 domain + 138 server unit/configuration + 47 PostgreSQL integration + 100 client-core + 140 React component + 25 real-server browser cases**. The newly added eight component cases and eleven server-browser journeys have not been executed on the pinned production stack here. No new 0.2.2 coverage percentage is claimed.

This authoring environment has no Maven, Docker or PostgreSQL; it supplies JDK 21/Node 22 and cannot resolve dependency registries. The full Spring/PostgreSQL/React 19 build must still be executed in CI. A locally reproduced UI repair is not a substitute for that release check.

## Run the full gate

From the repository root on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-all.ps1
```

Or with Docker Compose:

```bash
docker compose -f compose.quality.yml up --build --abort-on-container-exit --exit-code-from verifier
```

For a focused browser rerun against a running disposable test deployment:

```bash
cd frontend
npm run test:e2e -- tests/e2e/modules.spec.js tests/e2e/responsive-auth.spec.js
```

The focused rerun includes other module tests in `modules.spec.js`; it does not replace the full gate. Commit and push the corrected source to obtain a run on the new commit. Preserve `.env`, database volumes, intentional changes and an existing reviewed lockfile. Only package-version metadata should change in that lockfile; do not re-resolve dependency versions unnecessarily.

## Release safety

No Flyway migration, invoice workflow rule or server authorization changed. No data reset is needed. No live DATEV integration or compliance certification is claimed. Security/accessibility/privacy review, dependencies/licenses, production TLS/secrets, restore tests and accounting-handoff acceptance remain release requirements.

## Zusammenfassung auf Deutsch

Die neue Revision korrigiert die Erreichbarkeit der Abmeldung, die exakten Formularbeschriftungen und ein zusätzliches Rennen beim Sitzungswechsel. Der bereitgestellte 0.2.1-Lauf bestätigt 491 Backend- und 224 Frontend-Fälle; zwei von 14 Browser-Journeys scheiterten. Für 0.2.2 wurden lokal 306 Domänen-, 100 Client-, neun Berichtstests sowie 13 gezielte und 17 bisherige Kompatibilitätsszenarien ausgeführt. Der vollständige Lauf mit 140 React-Komponentenfällen und 25 echten Server-Browser-Journeys bleibt bis zur CI offen. Die Kompatibilitätsvorschau verwendet synthetische Daten und ersetzt diese Prüfung nicht. Keine Datenbankmigration oder Datenlöschung erforderlich.
