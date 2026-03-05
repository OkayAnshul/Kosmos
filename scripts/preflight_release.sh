#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPS="$ROOT_DIR/local.properties"
HOME_GRADLE="$HOME/.gradle/gradle.properties"

required=(
  SUPABASE_URL
  SUPABASE_ANON_KEY
  GOOGLE_WEB_CLIENT_ID
  RELEASE_STORE_FILE
  RELEASE_STORE_PASSWORD
  RELEASE_KEY_ALIAS
  RELEASE_KEY_PASSWORD
)

check_prop() {
  local key="$1"
  local value=""

  if [[ -f "$LOCAL_PROPS" ]]; then
    value="$(grep -E "^${key}=" "$LOCAL_PROPS" | tail -n1 | cut -d'=' -f2- || true)"
  fi

  if [[ -z "$value" && -f "$HOME_GRADLE" ]]; then
    value="$(grep -E "^${key}=" "$HOME_GRADLE" | tail -n1 | cut -d'=' -f2- || true)"
  fi

  if [[ -n "$value" ]]; then
    echo "[OK] $key"
    return 0
  fi

  echo "[MISSING] $key"
  return 1
}

echo "Kosmos release preflight"
echo "- root: $ROOT_DIR"

a=0
for key in "${required[@]}"; do
  if ! check_prop "$key"; then
    a=1
  fi
done

if [[ $a -eq 0 ]]; then
  echo
  echo "All required properties found."
  echo "Run: ./gradlew testDebugUnitTest lintRelease bundleRelease"
else
  echo
  echo "Missing properties detected."
  echo "Add them to local.properties or ~/.gradle/gradle.properties"
  exit 1
fi
