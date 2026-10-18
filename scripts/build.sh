#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
(cd frontend; if [ -f package-lock.json ]; then npm ci; else npm install; fi; npm run typecheck; npm test; npm run build)
rm -rf server/src/main/resources/static
mkdir -p server/src/main/resources/static
cp -R frontend/dist/. server/src/main/resources/static/
./mvnw --batch-mode --no-transfer-progress verify
