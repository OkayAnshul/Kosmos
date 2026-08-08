#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AAB_PATH="$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab"

if [[ ! -f "$AAB_PATH" ]]; then
  echo "[MISSING] $AAB_PATH"
  echo "Run: ./gradlew bundleRelease"
  exit 1
fi

if ! command -v jarsigner >/dev/null 2>&1; then
  echo "[MISSING] jarsigner command not found (requires JDK tools)."
  exit 1
fi

OUT="$(jarsigner -verify -verbose -certs "$AAB_PATH" 2>&1 || true)"

if echo "$OUT" | grep -qi "jar is unsigned"; then
  echo "[FAIL] Release bundle is unsigned."
  echo "Configure RELEASE_* properties and rebuild: ./gradlew bundleRelease"
  exit 1
fi

if echo "$OUT" | grep -qi "jar verified"; then
  echo "[OK] Release bundle signature verified: $AAB_PATH"
  exit 0
fi

echo "[WARN] Could not confirm signature status. Raw output:"
echo "$OUT"
exit 1
