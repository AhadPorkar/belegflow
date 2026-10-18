#!/bin/sh
# Full networked verification, never a pure-unit shortcut. Requires a disposable PostgreSQL database.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
[ "${BF_INTEGRATION:-false}" = true ] || { echo 'BLOCKED: BF_INTEGRATION=true is required.' >&2; exit 1; }
[ "${BF_TEST_DATABASE_ACK:-}" = DISPOSABLE ] || { echo 'BLOCKED: explicitly acknowledge BF_TEST_DATABASE_ACK=DISPOSABLE.' >&2; exit 1; }
case "${BF_DATABASE_URL:-}" in *'/belegflow_test') ;; *) echo 'BLOCKED: use the dedicated database named belegflow_test, never customer data.' >&2; exit 1;; esac
python3 -m unittest discover -s scripts/tests -p 'test_*.py' -v
mkdir -p quality-results
SERVER_PID=''
collect() {
  code=$?
  trap - EXIT HUP INT TERM
  if [ -n "$SERVER_PID" ]; then kill "$SERVER_PID" 2>/dev/null || true; fi
  for module in domain server; do
    mkdir -p "quality-results/$module"
    [ ! -d "$module/target/surefire-reports" ] || cp -R "$module/target/surefire-reports" "quality-results/$module/"
    [ ! -d "$module/target/site/jacoco" ] || cp -R "$module/target/site/jacoco" "quality-results/$module/"
  done
  for directory in coverage test-results playwright-report; do
    [ ! -d "frontend/$directory" ] || cp -R "frontend/$directory" quality-results/
  done
  [ ! -f frontend/package-lock.json ] || cp frontend/package-lock.json quality-results/
  printf '%s\n' "$code" > quality-results/exit-code.txt
  exit "$code"
}
trap collect EXIT HUP INT TERM
export BF_DEMO=true BF_COOKIE_SECURE=false BF_REMINDER_INITIAL_DELAY_MS=3600000 BF_REMINDER_DELAY_MS=3600000
(cd frontend; if [ -f package-lock.json ]; then npm ci; else
  echo 'First dependency resolution: review and commit the generated lockfile before a release.'; npm install; fi
  npm run typecheck; npm run test:coverage; npm run build)
rm -rf server/src/main/resources/static
mkdir -p server/src/main/resources/static
cp -R frontend/dist/. server/src/main/resources/static/
PROFILE=''
[ "${BF_STRICT_COVERAGE:-false}" != true ] || PROFILE='-Prelease-quality'
# Intentional word splitting permits the optional, controlled profile flag only.
./mvnw --batch-mode --no-transfer-progress $PROFILE verify
python3 scripts/assert-integration-reports.py
java -jar server/target/belegflow-server-0.2.1.jar > quality-results/server.log 2>&1 &
SERVER_PID=$!
ready=false
for i in $(seq 1 90); do
  if curl --fail --silent http://localhost:8080/actuator/health >/dev/null; then ready=true; break; fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then break; fi
  sleep 2
done
[ "$ready" = true ] || { cat quality-results/server.log; echo 'Application did not become healthy.' >&2; exit 1; }
(cd frontend && npm run test:e2e)
echo 'All requested automated stages completed. Review coverage and artifacts in quality-results/.'
