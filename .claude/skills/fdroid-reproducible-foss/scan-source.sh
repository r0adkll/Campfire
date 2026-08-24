#!/usr/bin/env bash
# Run F-Droid's real source scanner (fdroidserver master, the exact build CI uses) against a
# Campfire git ref with the `scandelete` list applied — the reliable way to confirm the foss source
# scans clean before pushing metadata or cutting a release. Requires Docker.
#
# Usage:
#   scan-source.sh [git-ref]      # default: HEAD
#
# Keep SCANDELETE identical to the `scandelete:` block in the fdroiddata metadata.
set -euo pipefail

REF="${1:-HEAD}"

# The files F-Droid deletes before scanning (must match metadata/app.campfire.android.yml).
SCANDELETE=(
  "data/crashreporting/firebase/build.gradle.kts"
  "infra/audioplayer/cast/build.gradle.kts"
  "ui/theming/ai/build.gradle.kts"
  "gradle/build-logic/firebase/build.gradle.kts"
  "gradle/emulatorwtf-repo.gradle.kts"
)

REPO_ROOT="$(git rev-parse --show-toplevel)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "Exporting $REF …"
git -C "$REPO_ROOT" archive "$REF" | tar -x -C "$WORK"

echo "Applying scandelete (${#SCANDELETE[@]} files) …"
for f in "${SCANDELETE[@]}"; do
  if [[ -e "$WORK/$f" ]]; then rm -f "$WORK/$f"; else echo "  WARN: not present: $f"; fi
done

echo "Running fdroidserver master scan_source() in Docker …"
docker run --rm -v "$WORK":/src debian:trixie-slim bash -c '
  set -e
  apt-get update -qq >/dev/null 2>&1
  apt-get install -qy --no-install-recommends fdroidserver curl ca-certificates git python3-yaml >/dev/null 2>&1
  rm -rf /fds && mkdir /fds
  curl --silent https://gitlab.com/fdroid/fdroidserver/-/archive/master/fdroidserver-master.tar.gz \
    | tar -xz --directory=/fds --strip-components=1
  export PYTHONPATH="/fds:/fds/examples"
  python3 - <<PY
import argparse
from fdroidserver import scanner, common, metadata
common.config = {}
# handleproblem is silent (no log, no json) when get_options() is None but still counts;
# force real options so every counted problem is recorded, then print them.
opts = argparse.Namespace(verbose=True, json=True)
common.options = opts
common.get_options = lambda: opts
store = scanner.MessageStore()
count = scanner.scan_source("/src", build=metadata.Build(), json_per_build=store)
print("=====================================")
print("SCAN COUNT =", count)
for e in store.errors:
    print("  ERROR:", e)
print("=====================================")
PY
'
