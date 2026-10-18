#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
[ -f frontend/package-lock.json ] || { echo 'BLOCKED: resolve, review and commit frontend/package-lock.json.' >&2; exit 1; }
export BF_STRICT_COVERAGE=true
./scripts/verify-all.sh
echo 'Automated release gates completed. Independent security, accessibility, restore and legal review remain required.'
