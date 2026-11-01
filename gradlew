#!/usr/bin/env sh
# Lightweight, auditable Gradle bootstrap: fetches pinned Gradle if not installed.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VER=8.13
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$ROOT" "$@"
fi
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/josjis-bootstrap"
HOME_GRADLE="$CACHE/gradle-$VER/bin/gradle"
if [ ! -x "$HOME_GRADLE" ]; then
  mkdir -p "$CACHE"
  ARCHIVE="$CACHE/gradle-$VER-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VER-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 2 --output "$ARCHIVE" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget --output-document="$ARCHIVE" "$URL"
  else
    echo 'Please install Gradle 8.13 or curl/wget.' >&2; exit 1
  fi
  unzip -qo "$ARCHIVE" -d "$CACHE"
  rm -f "$ARCHIVE"
fi
exec "$HOME_GRADLE" -p "$ROOT" "$@"
