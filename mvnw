#!/bin/sh
# Small project-owned Maven launcher, not the Apache Maven Wrapper implementation.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=3.10.0
HOME_DIR="$ROOT/.tools/apache-maven-$VERSION"
if [ ! -x "$HOME_DIR/bin/mvn" ]; then
  command -v curl >/dev/null || { echo 'curl is required to bootstrap Maven.' >&2; exit 1; }
  mkdir -p "$ROOT/.tools"
  TEMP=$(mktemp -d "$ROOT/.tools/bootstrap.XXXXXX")
  trap 'rm -rf "$TEMP"' EXIT HUP INT TERM
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$VERSION/apache-maven-$VERSION-bin.tar.gz"
  curl --fail --location --proto '=https' --tlsv1.2 "$URL" -o "$TEMP/maven.tar.gz"
  curl --fail --location --proto '=https' --tlsv1.2 "$URL.sha512" -o "$TEMP/maven.sha512"
  EXPECTED=$(awk '{print $1}' "$TEMP/maven.sha512" | tr '[:upper:]' '[:lower:]')
  if command -v sha512sum >/dev/null; then ACTUAL=$(sha512sum "$TEMP/maven.tar.gz" | awk '{print $1}');
  elif command -v shasum >/dev/null; then ACTUAL=$(shasum -a 512 "$TEMP/maven.tar.gz" | awk '{print $1}');
  else echo 'sha512sum or shasum is required.' >&2; exit 1; fi
  [ "$EXPECTED" = "$ACTUAL" ] || { echo 'Maven download checksum mismatch.' >&2; exit 1; }
  tar -xzf "$TEMP/maven.tar.gz" -C "$ROOT/.tools"
fi
exec "$HOME_DIR/bin/mvn" "$@"
