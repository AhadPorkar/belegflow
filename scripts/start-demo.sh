#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
command -v docker >/dev/null || { echo 'Install Docker Engine/Desktop with Compose first.' >&2; exit 1; }
if [ ! -f .env ]; then
  umask 077
  if command -v openssl >/dev/null; then SECRET=$(openssl rand -hex 32);
  else SECRET=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n'); fi
  printf 'BF_DATABASE_PASSWORD=%s\nBF_DEMO=true\nBF_COOKIE_SECURE=false\n' "$SECRET" > .env
fi
if grep -q 'REPLACE_WITH' .env; then echo 'Replace the placeholder secret in .env first.' >&2; exit 1; fi
docker compose up --build -d --wait
echo 'Open http://localhost:8080. Finance: finance@belegflow.demo / BelegFlow-Demo-2026!'
echo 'Local demo only. Never expose demo credentials to the public internet.'
