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

cd "$ROOT_DIR"
# Beta: skip the (Spring Boot 4-incompatible) test compilation for now.
mvn -Dmaven.test.skip=true spring-boot:run
