# Installation and operations

[Deutsch](../de/OPERATIONS.md) | This is a single-instance pilot runbook, not a claim of production certification.

## Local demonstration

Use the root start scripts. They generate a private random database password in ignored `.env`, build the assets/JAR, start PostgreSQL and the application, and wait for health checks. Required software: Docker with Compose and outbound access to npm/Maven/container registries. Native development additionally needs Java 25, Node 24.22.0 and the Maven launcher prerequisites. Approximate hardware needs must be measured; no production capacity benchmark was executed.

Demo defaults: HTTP only on `127.0.0.1:8080`, `BF_DEMO=true`, secure cookies disabled for local HTTP. Database has no exposed host port unless `compose.dev.yml` is explicitly added. The PostgreSQL 18 volume mount is `/var/lib/postgresql`. Never use `docker compose down -v` on data you need; `-v` destroys the database volume.

To stop without deleting data: `docker compose down`. To inspect startup: `docker compose logs --tail=150 app db`. Health: `curl --fail http://localhost:8080/actuator/health`. No health details, credentials or database query text should be publicly exposed.

## Native development

```bash
# First create .env with a unique private database password.
docker compose -f compose.yml -f compose.dev.yml up -d db
export BF_DATABASE_URL=jdbc:postgresql://localhost:5432/belegflow
export BF_DATABASE_USER=belegflow
export BF_DATABASE_PASSWORD='the-same-private-password'
export BF_DEMO=true
export BF_COOKIE_SECURE=false
./mvnw install -DskipTests
./mvnw -pl server spring-boot:run
```

In a second terminal run `cd frontend && npm install && npm run dev`. Open the Vite address printed in that terminal; `/api` is proxied to 8080. To build a single executable server containing production web assets use `sh scripts/build.sh`. Do not set `BF_INTEGRATION=true` against a real customer database: integration tests assume synthetic data and rollback most operations but application initialization is not a backup strategy.

## Environment variables

| Variable | Meaning |
|---|---|
| BF_DATABASE_URL | JDBC PostgreSQL URL; production must use the approved network/TLS configuration. |
| BF_DATABASE_USER / BF_DATABASE_PASSWORD | DB credentials; never commit production values. |
| BF_DEMO | Defaults false in the application. True seeds public synthetic identities on an empty DB. |
| BF_COOKIE_SECURE | Defaults true. False is for local HTTP only. |
| BF_BOOTSTRAP_EMAIL | Initial administrator email for a non-demo, empty installation. |
| BF_BOOTSTRAP_PASSWORD | At least 16 characters for initial bootstrap; use a unique random 24+ character secret. |
| BF_BOOTSTRAP_ORGANIZATION | Initial organization name. |
| BF_FLYWAY_ENABLED | Defaults true. Disable only when a separate controlled migration step is implemented. |
| BF_REMINDER_DELAY_MS | Default 900000: 15-minute in-app reminder schedule. |
| PORT | HTTP port, default 8080; supplied healthcheck assumes 8080. |
| BF_INTEGRATION | Test-only opt-in to run the real database integration class. |

Bootstrap only runs against an empty organization table. Changing environment passwords later does not change existing account passwords. Account changes use the administrator user interface. Do not change the database secret in `.env` and expect PostgreSQL to rotate an existing volume's account; rotate inside PostgreSQL with an approved procedure and then update the application secret.

## Controlled pilot deployment

Complete the release gates before any real customer data. Use a different host/project/volume from the public-credential demo. Copy `.env.production.example` to a private environment file, replace all placeholders and restrict access. The production Compose file is standalone, not an override of the demo:

```bash
docker compose --env-file /secure/path/belegflow.env \
  -f compose.production.yml up --build -d --wait
```

The application is bound to host loopback. Put an independently configured HTTPS reverse proxy in front of it, with a real certificate, approved domain, request/body/time limits and rate limiting. Forward `/api` and static assets consistently to the same origin. Secure session cookies require HTTPS. Do not set permissive cross-origin access or expose PostgreSQL/management interfaces. The supplied login limiter uses the direct remote address; a proxy may collapse clients to one address. Configure and test edge rate limiting explicitly rather than trusting arbitrary forwarded headers.

Bootstrap secrets remain readable to privileged container operators through environment inspection; use an approved secret-management deployment before commercial operation. Remove bootstrap values from long-lived configuration after successful initialization and restarting safely; an existing organization does not need them. Never remove the only verified administrator access.

The example DB account owns the schema so Flyway can migrate it. This is acceptable only as a documented local/pilot convenience. A hardened release should separate a migration/owner role from a runtime role, grant runtime only the required table/sequence rights, and ensure it cannot alter schema or disable append-only triggers. No ready-made least-privilege/RLS deployment is claimed here. Test migrations and application behavior under the actual chosen roles.

Only one application instance is supported by this runbook. Sessions and the login limiter are in-memory. A restart signs users out. Multiple replicas require deliberate shared-session, scheduling and rate-limiting design. Do not put multiple replicas behind a load balancer and assume HA from Docker alone.

## Organization setup

Create Finance, approver and administrator accounts using Workspace settings. Use unique email addresses; an email identifies a single organization member in this version. Create suppliers, projects with responsible approvers, and cost centers. Set the gross threshold for second approval and define time-limited delegations as needed. Confirm a non-creator independent administrator is available for high-value invoices. This rule can intentionally block a poorly configured organization instead of silently bypassing separation of duties.

There is no self-service tenant signup, billing portal or invitation email. One organization is created per non-demo bootstrap installation. The schema/services carry tenant IDs and the demo includes another isolated tenant for tests, but provisioning additional organizations is not a user-facing product feature.

## Backups and restoration

Documents and export packages are stored in PostgreSQL `bytea`; a consistent database backup covers both metadata and those bytes. Local demo helper: `sh scripts/backup.sh`. It writes a restrictive-permission custom-format dump, not an encrypted off-site backup. Encrypt approved copies, transfer them off-host, set retention and monitor freshness. Include secrets/configuration separately under secure management.

Verify every backup strategy by restoring into a **separate disposable** database, never over the only production copy. Example commands for a temporary database in an approved test environment:

```bash
# Use the matching compose/env options for the intended source environment.
docker compose exec -T db createdb -U belegflow belegflow_restore_check
cat backups/your-verified.dump | docker compose exec -T db \
  pg_restore -U belegflow -d belegflow_restore_check --no-owner --exit-on-error
```

Start a separate test application pointing at that database on another port. Check organization/user counts, invoice states, original-document hashes, an export download and the audit timeline. Record elapsed restoration time and data currency. RPO/RTO are business decisions; none are measured or promised by this package. Clean up only the disposable restore database after its checks and approval.

## Upgrades and rollback

Review changes and dependency advisories; resolve/review the npm lock and image digests; run the full verification pipeline. Back up, restore-test, deploy to staging, run schema migrations and representative workflows, then release through a maintenance plan. Never edit an already deployed Flyway migration; add a new version. A JAR rollback may not be safe after an irreversible migration. Restore to a separate verified environment or use a tested forward fix, not an improvised destructive SQL command.

## Logging, monitoring and recovery

Monitor health, HTTP error rates, response latency, DB space/connections, failed logins and backup age. The package exposes minimal health only; it does not include a hosted monitoring service, alert routing or full OpenTelemetry deployment. Unexpected API errors log a reference and exception class without SQL messages or confidential invoice bodies. Correlate that reference with time and request context under approved access.

Reminders are database-backed and deduplicated per day but not a distributed job scheduler. Export retries use persisted idempotency keys and packages; retain the same key and payload after a lost response. Invoice commands use optimistic versions; after a conflict reload rather than automatically approving stale data.

## Privacy, retention and incident handling

Decide controller/processor responsibilities, hosting access, subprocessors, applicable transfer safeguards, customer terms and retention with qualified reviewers. EU hosting by itself is not a compliance certification. There is no embedded analytics or outbound AI flow in the default application.

Document/audit/export rows are protected from ordinary updates/deletes by database triggers. This is not WORM storage, cryptographic tamper-proofing against a DBA, or a complete legal archive. Automatic retention deletion, legal holds, individual-data erasure and organization purge are not implemented. Define a reviewed policy and implement/test the needed administrative procedures before real personal data is processed. Deleting an entire database to 'solve' an erasure request is not an acceptable routine.

For suspected compromise: restrict access, preserve appropriate evidence, rotate compromised secrets through controlled procedures, assess affected records/organizations, and invoke the organization's notification/legal process. Do not publish confidential invoice contents in GitHub issues. See SECURITY.md and docs/en/RELEASE.md.
