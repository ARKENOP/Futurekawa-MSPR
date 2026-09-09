#!/usr/bin/env bash
# Loads backend-central/.env (if present) into the environment, then starts Spring Boot.
# Mirrors backend-local/scripts/run-dev.sh.
# Usage:  ./scripts/run-dev.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$ROOT_DIR/.env"

# Unlike backend-local, every setting has a usable default in application.yml,
# so a missing .env is fine for a local run against localhost backends.
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  echo "Loaded $ENV_FILE"
else
  echo "No .env found — using the defaults from application.yml."
fi

echo "Starting backend-central ..."
echo "  locals : ${LOCAL_BR_URL:-http://localhost:8081}, ${LOCAL_EC_URL:-http://localhost:8082}, ${LOCAL_CO_URL:-http://localhost:8083}"
echo "  port   : ${SERVER_PORT:-8090}"

# backend-central depends on futurekawa-lib, so make the library resolvable first.
REPO_ROOT="$(dirname "$ROOT_DIR")"
mvn -B -q -f "$REPO_ROOT/pom.xml" -pl futurekawa-lib -am install

cd "$ROOT_DIR"
# Tests are skipped here for startup speed; run them with `mvn verify` at the root.
mvn -Dmaven.test.skip=true spring-boot:run
