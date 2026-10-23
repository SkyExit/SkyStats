#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VERSION=3.9.11
MAVEN="$ROOT/.tools/apache-maven-$VERSION"
if [[ ! -x "$MAVEN/bin/mvn" ]]; then
  mkdir -p "$ROOT/.tools"
  ARCHIVE="$ROOT/.tools/apache-maven-$VERSION-bin.tar.gz"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$VERSION/apache-maven-$VERSION-bin.tar.gz"
  curl --fail --location --retry 3 "$URL" -o "$ARCHIVE"
  curl --fail --location --retry 3 "$URL.sha512" -o "$ARCHIVE.sha512"
  EXPECTED="$(tr -d '[:space:]' < "$ARCHIVE.sha512")"
  ACTUAL="$(sha512sum "$ARCHIVE" | cut -d ' ' -f 1)"
  [[ "$EXPECTED" == "$ACTUAL" ]] || { echo 'Maven checksum mismatch' >&2; exit 1; }
  tar -xzf "$ARCHIVE" -C "$ROOT/.tools"
fi
exec "$MAVEN/bin/mvn" -Dmaven.repo.local="$ROOT/.tools/m2" -f "$ROOT/pom.xml" "$@"
