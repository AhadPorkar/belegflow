#!/bin/sh
# Demo deployment helper. For production use the matching private env and compose file.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
umask 077
mkdir -p backups
FILE="backups/belegflow-$(date -u +%Y%m%dT%H%M%SZ).dump"
TEMP="$FILE.partial"
trap 'rm -f "$TEMP"' EXIT HUP INT TERM
docker compose exec -T db pg_dump -U belegflow -d belegflow -Fc > "$TEMP"
mv "$TEMP" "$FILE"
printf 'Created %s. Encrypt and copy off-host; verify a restore into a separate database.\n' "$FILE"
