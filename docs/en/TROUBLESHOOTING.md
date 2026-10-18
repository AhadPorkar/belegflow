# Troubleshooting and upgrading to 0.2.1

[Deutsch](../de/TROUBLESHOOTING.md) | [Testing](TESTING.md) | [Operations](OPERATIONS.md)

## New run 106020289651: multipart test compilation

This is **not a recurrence of the Jackson startup error**. Maven failed in `testCompile`, before the selected Jackson tests could run:

```text
ModuleIntegrationTest.java:[53,93] incompatible types:
MockMultipartHttpServletRequestBuilder cannot be converted to MockHttpServletRequestBuilder
```

The helper in `server/src/test/java/com/belegflow/server/ModuleIntegrationTest.java` must return its actual multipart builder type:

```java
// Before:
MockHttpServletRequestBuilder pdf(String number) {
    // multipart(...).file(...).param(...)
}

// After:
MockMultipartHttpServletRequestBuilder pdf(String number) {
    // The existing body is unchanged.
}
```

The multipart and ordinary builders are separate concrete types in the current Spring API. Shared fluent operations do not make one a subtype of the other. Do not add a cast, drop the multipart file, downgrade Spring, or disable test compilation.

Version 0.2.1 adds `MultipartRequestBuilderTest` (six database-free cases) and splits the CI preflight into an explicit compile step followed by the Jackson/multipart regressions. All database and real-browser suites are retained.

The same uploaded run confirms **224 frontend tests passed**, along with type checking, production build and the npm audit gate. It reports **87.38% lines / 84.82% branches** for the frontend. The full-stack job had no successful execution because its backend prerequisite failed. See [the source-specific evidence](../testing/CI-106020289651.md).

### Apply the repair safely

Replace or merge the contents of the ZIP's `belegflow/` folder into the existing repository root, not into a nested second folder. Keep your `.env`, deployment secrets, database volumes, intentional code changes and existing reviewed `frontend/package-lock.json`. The application metadata is now 0.2.1; update an existing lockfile's top-level package version accordingly. No dependency version was changed by this patch.

A rerun of the **old GitHub run** still tests the old commit. Commit and push the corrected files so a new run uses the repair, or select the corrected branch for a manual workflow dispatch.

There is **no database migration** in this repair. Do not run `docker compose down -v` or remove your PostgreSQL volume to address a Java test-compiler error.

```bash
# Focused check, JDK 25 and access to Maven repositories required:
./mvnw --batch-mode --no-transfer-progress test-compile
./mvnw --batch-mode --no-transfer-progress -Dtest=JacksonConfigurationTest,MultipartRequestBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

`MultipartRequestBuilderTest` uses the actual test helper with mocked SQL lookup values. It does not connect to PostgreSQL and does not replace the import integration tests. Its execution in the pinned environment remains required; see [the verification boundary](../testing/VERIFICATION.md).

Official API reference: https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/test/web/servlet/request/MockMultipartHttpServletRequestBuilder.html


## Earlier run 106009699774: Jackson configuration startup error

The first meaningful error in `full-stack/8_Spring, domain and real PostgreSQL integration tests.txt` is a property-binding failure:

```text
Failed to bind properties under 'spring.jackson.serialization'
No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

This is one application-startup failure, not fifteen separate business-rule failures. The subsequent context-failure-threshold messages are consequences. The uploaded run records 82 passing domain tests, 12 passing server adapter tests and 15 integration cases that failed to start. Its 24 client core tests and production frontend build also passed; those older results do not certify this changed release.

In `server/src/main/resources/application.yml`, remove the legacy `serialization.write-dates-as-timestamps` entry and retain:

```yaml
spring:
  jackson:
    datatype:
      datetime:
        write-dates-as-timestamps: false
```

Jackson 3 moved this feature out of `SerializationFeature`. Changing `false` to `true`, increasing Spring's failure threshold, disabling the integration class or adding Jackson 2 does not address the mismatch. Also remove any stale `SPRING_JACKSON_SERIALIZATION_WRITE_DATES_AS_TIMESTAMPS` environment override. Keep dates and instants as ISO text; `JacksonConfigurationTest` guards both shapes and the actual YAML.

## Upgrade the project, not the database

Back up any database that matters. Extract the revised repository into a clean directory. Copy your deployment secrets from a secure location, not from Git. Compare and retain intentional local changes before replacing files. Do not nest a second `belegflow/` inside your existing repository. Commit the revised source, tests, workflows and documentation together. Preserve the existing application's database volume; there is no schema change in 0.2.0.

Run the dedicated quality stack first. Then rebuild the application with `docker compose up --build -d`. Inspect `docker compose logs app`. Do not run `docker compose down -v` against a valuable demo or production installation as a way to fix Jackson. The quality stack has its own name, no application data volume, and a disposable test database.

On GitHub, the first job is the database-free configuration regression. The second checks frontend units/components/build. The final job requires both and runs PostgreSQL plus the real browser. Download artifacts from the failed job and start with the earliest cause, not the final reactor summary.

## Common failures

| Symptom | Diagnosis and next step |
|---|---|
| New Jackson error still mentions `serialization` | Search YAML, environment and deployment overrides for the old property. Rebuild the image containing the revised YAML. |
| Integration tests show skipped | Use the dedicated quality command with `BF_INTEGRATION=true`; ordinary `mvn verify` is not a complete verification run. |
| Docker image or npm/Maven cannot download | Check registry access, proxy, DNS and the required toolchain. No fake-success fallback is provided. |
| PostgreSQL connection refused | Wait for the disposable service's health check and inspect its logs; confirm the database is named `belegflow_test`. |
| TypeScript/Vitest cannot be found | Install dependencies using the configured Node release. Commit a reviewed lockfile, then use `npm ci`. |
| Coverage gate fails | Open the HTML report and add meaningful tests for missing branches. Do not lower thresholds simply to obtain a green badge. |
| Browser login stays on the form | Inspect status/error, cookie scope and TLS setting. Local HTTP requires the explicit demo/test cookie configuration. |
| PDF import returns `INVALID_INPUT` | Supply a supported supplier, invoice number, valid issue/due dates and exact amounts; date-only API format is `YYYY-MM-DD`. |
| Invoice command returns `VERSION_CONFLICT` | Reload the invoice and inspect the history before deciding whether to repeat the action. |
| Export retry returns an idempotency conflict | Reuse the same key only with the same invoice IDs and versions. A changed request needs a new user-reviewed operation. |
| Silent missing notifications | Check assignment, due date, role and read state; the feature is in-app, not email. |

Never paste passwords, cookies, complete invoices or confidential source files into a public issue. Include the version, command, first exception, reference ID and redacted timestamps.

## Technical references

* Spring Boot JSON reference: https://docs.spring.io/spring-boot/reference/features/json.html
* Jackson properties API: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jackson/autoconfigure/JacksonProperties.Datatype.html
* Jackson 3 datetime feature: https://javadoc.io/doc/tools.jackson.core/jackson-databind/latest/tools.jackson.databind/tools/jackson/databind/cfg/DateTimeFeature.html
