#!/usr/bin/env bash
#
# backend/deploy/deploy.sh
#
# Runs ON kirin-server via SSH. Deploys a new backend jar, restarts the
# systemd service, health-checks it, and automatically rolls back to the
# previous jar if the new deployment does not become healthy.
#
# Usage: deploy.sh <path-to-new-jar>
#
# Required environment variables (exported into the SSH session by the
# caller):
#   SPRING_DATASOURCE_URL
#   SPRING_DATASOURCE_USERNAME
#   SPRING_DATASOURCE_PASSWORD
#
# Optional environment variables (Slack 에러 알림 — 비어 있으면 기존처럼 비활성):
#   SLACK_ENABLED
#   SLACK_BOT_TOKEN
#   SLACK_CHANNEL
#
# NOTE: the app's own controllers already declare their "/api" prefix in
# code (e.g. @RequestMapping("/api/lockers")), so we deliberately do NOT set
# server.servlet.context-path here (that would double it up to /api/api/...).
# Actuator is instead given its own base path so /api/actuator/health still
# works for the health check and for nginx's /api/ passthrough proxy.
#
# Emits exactly one line matching ^RESULT=<marker>$ to stdout before exiting.
# Markers: success | rolled_back | rollback_unverified | rollback_failed |
#          no_rollback_target | precondition_failed

set -uo pipefail

APP_DIR="/home/ec2-user/kirin-backend"
CURRENT_JAR="${APP_DIR}/app.jar"
PREV_JAR="${APP_DIR}/app.jar.prev"
ENV_FILE="${APP_DIR}/backend.env"
LOCK_FILE="${APP_DIR}/deploy.lock"
SERVICE_NAME="kirin-backend"
HEALTH_URL="http://127.0.0.1:8080/api/actuator/health"
HEALTH_ATTEMPTS=30
HEALTH_INTERVAL=3

NEW_JAR="${1:-}"

log() { echo "[deploy] $*"; }

fail() {
  echo "RESULT=$1"
  exit 1
}

mkdir -p "$APP_DIR"

exec 200>"$LOCK_FILE"
if ! flock -n 200; then
  log "Another deploy is already in progress, aborting."
  fail "precondition_failed"
fi

if [ -z "$NEW_JAR" ] || [ ! -s "$NEW_JAR" ]; then
  log "New jar '$NEW_JAR' is missing or empty."
  fail "precondition_failed"
fi

if [ -z "${SPRING_DATASOURCE_URL:-}" ] || [ -z "${SPRING_DATASOURCE_USERNAME:-}" ] || [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
  log "Required datasource environment variables are not set."
  fail "precondition_failed"
fi

log "Writing environment file to ${ENV_FILE}"
UMASK_OLD=$(umask)
umask 077
TMP_ENV_FILE=$(mktemp "${APP_DIR}/.backend.env.XXXXXX")
cat > "$TMP_ENV_FILE" <<EOF
SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}
TOSS_SECRET_KEY=${TOSS_SECRET_KEY:-}
SLACK_ENABLED=${SLACK_ENABLED:-false}
SLACK_BOT_TOKEN=${SLACK_BOT_TOKEN:-}
SLACK_CHANNEL=${SLACK_CHANNEL:-}
MANAGEMENT_ENDPOINTS_WEB_BASE_PATH=/api/actuator
EOF
mv "$TMP_ENV_FILE" "$ENV_FILE"
chmod 600 "$ENV_FILE"
umask "$UMASK_OLD"

health_check() {
  local attempt=1
  while [ "$attempt" -le "$HEALTH_ATTEMPTS" ]; do
    if curl -fsS --max-time 2 "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
      return 0
    fi
    log "Health check attempt ${attempt}/${HEALTH_ATTEMPTS} failed, retrying in ${HEALTH_INTERVAL}s..."
    sleep "$HEALTH_INTERVAL"
    attempt=$((attempt + 1))
  done
  return 1
}

restart_service() {
  sudo systemctl daemon-reload && sudo systemctl restart "$SERVICE_NAME"
}

HAD_PREV_JAR=false
log "Backing up current jar (if any) to ${PREV_JAR}"
if [ -f "$CURRENT_JAR" ]; then
  cp -f "$CURRENT_JAR" "$PREV_JAR"
  HAD_PREV_JAR=true
fi

log "Installing new jar at ${CURRENT_JAR}"
cp -f "$NEW_JAR" "$CURRENT_JAR"

log "Restarting ${SERVICE_NAME}"
if ! restart_service; then
  log "systemctl restart failed for the new jar."
  if [ "$HAD_PREV_JAR" != "true" ]; then
    fail "no_rollback_target"
  fi
  cp -f "$PREV_JAR" "$CURRENT_JAR"
  if ! restart_service; then
    fail "rollback_failed"
  fi
  if health_check; then
    fail "rolled_back"
  else
    fail "rollback_unverified"
  fi
fi

log "Waiting for health check at ${HEALTH_URL}"
if health_check; then
  log "New deployment is healthy."
  rm -f "${NEW_JAR}"
  echo "RESULT=success"
  exit 0
fi

log "New deployment failed health check, rolling back."
if [ "$HAD_PREV_JAR" != "true" ]; then
  log "No previous jar available to roll back to."
  fail "no_rollback_target"
fi

cp -f "$PREV_JAR" "$CURRENT_JAR"
if ! restart_service; then
  fail "rollback_failed"
fi

if health_check; then
  log "Rollback successful, previous jar is healthy again."
  fail "rolled_back"
else
  log "Rollback restart succeeded but service is still unhealthy."
  fail "rollback_unverified"
fi
