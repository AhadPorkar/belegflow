#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
[ -f frontend/package-lock.json ] || { echo 'BLOCKED: resolve, review and commit frontend/package-lock.json.' >&2; exit 1; }
[ "${BF_INTEGRATION:-false}" = true ] || { echo 'BLOCKED: set BF_INTEGRATION=true and supply a real PostgreSQL test database.' >&2; exit 1; }
./scripts/build.sh
(cd frontend && npm run test:e2e)
echo 'Automated checks completed. Human security, deployment, restore and legal review still required.'
