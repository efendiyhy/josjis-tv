#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
javac -d "$tmp" app/src/main/java/com/josjis/tv/FramexLinks.java tests/FramexLinksTest.java
java -cp "$tmp" FramexLinksTest
