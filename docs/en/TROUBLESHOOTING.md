# Troubleshooting and upgrading to 0.2.0

[Deutsch](../de/TROUBLESHOOTING.md) | [Testing](TESTING.md) | [Operations](OPERATIONS.md)

## The error in the uploaded GitHub Actions run

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
