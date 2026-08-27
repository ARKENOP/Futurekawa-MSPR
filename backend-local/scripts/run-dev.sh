#!/usr/bin/env bash
# Loads backend-local/.env into the environment, then starts Spring Boot.
# Usage:  ./scripts/run-dev.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: $ENV_FILE not found." >&2
  exit 1
fi

# Export every variable defined in .env (set -a marks them for export).
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "Starting backend-local for COUNTRY_CODE=${COUNTRY_CODE} ..."
echo "  MQTT : ${MQTT_BROKER_URL}"
echo "  DB   : jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"

# backend-local is a module of the futurekawa-parent reactor and depends on
# futurekawa-lib, so the library has to be resolvable before the module runs on
# its own. Installing it is near-instant (a handful of records and enums).
REPO_ROOT="$(dirname "$ROOT_DIR")"
mvn -B -q -f "$REPO_ROOT/pom.xml" -pl futurekawa-lib -am install

cd "$ROOT_DIR"
# Tests are skipped here for startup speed; run them with `mvn verify` at the root.
mvn -Dmaven.test.skip=true spring-boot:run
