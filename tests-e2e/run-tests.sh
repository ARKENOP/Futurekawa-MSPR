#!/usr/bin/env bash
#
# Runs the UI recette end to end from a cold checkout:
#   1. builds the frontend with the MSW fixtures enabled,
#   2. serves the build on 4173,
#   3. runs the Selenium suite against it,
#   4. tears the server down whatever the outcome.
#
# Usage:
#   tests-e2e/run-tests.sh                 # headless, full suite
#   tests-e2e/run-tests.sh -k fifo         # any pytest argument is forwarded
#   E2E_HEADED=1 tests-e2e/run-tests.sh    # watch the browser (demo)
#
# Against an already-running frontend, skip this script and run pytest directly:
#   E2E_BASE_URL=http://localhost:5173 pytest tests-e2e -v
#
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RACINE="$(cd "$ICI/.." && pwd)"
FRONT="$RACINE/frontend-web"
PORT="${E2E_PORT:-4173}"
BASE_URL="http://localhost:$PORT"

if command -v pnpm >/dev/null 2>&1; then
  PM=pnpm
elif command -v npm >/dev/null 2>&1; then
  PM=npm
else
  echo "Neither pnpm nor npm is available: cannot build the frontend." >&2
  exit 2
fi

echo "==> Frontend dependencies ($PM)"
cd "$FRONT"
[ -d node_modules ] || "$PM" install

echo "==> Build with the MSW fixtures enabled"
# The recette must not depend on the country backends: VITE_USE_MOCKS makes the
# browser answer its own API calls from frontend-web/src/mocks, which is what
# keeps the run deterministic and runnable in CI.
VITE_USE_MOCKS=true "$PM" run build

echo "==> Serving $BASE_URL"
VITE_USE_MOCKS=true "$PM" run preview -- --port "$PORT" --strictPort >/tmp/futurekawa-e2e-preview.log 2>&1 &
PREVIEW_PID=$!
trap 'kill "$PREVIEW_PID" 2>/dev/null || true' EXIT

for _ in $(seq 1 60); do
  if curl -sfo /dev/null "$BASE_URL"; then break; fi
  sleep 0.5
done
if ! curl -sfo /dev/null "$BASE_URL"; then
  echo "The preview server never came up. Log:" >&2
  cat /tmp/futurekawa-e2e-preview.log >&2
  exit 1
fi

echo "==> Selenium recette"
cd "$RACINE"
python3 -m pip install --quiet --disable-pip-version-check -r "$ICI/requirements.txt"
E2E_BASE_URL="$BASE_URL" python3 -m pytest "$ICI" -v "$@"
