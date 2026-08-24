#!/usr/bin/env bash
#
# frontend/deploy/deploy.sh
#
# Runs ON kirin-server via SSH. Atomically swaps in a newly built frontend
# dist/ directory, then runs smoke checks (homepage + backend proxy through
# nginx). Rolls back to the previous release symlink target on failure.
#
# Usage: deploy.sh <incoming-dist-dir> <release-id>
#
# Emits exactly one line matching ^RESULT=<marker>$ to stdout before exiting.
# Markers: success | rolled_back | rollback_unverified | rollback_failed |
#          no_rollback_target | precondition_failed

set -uo pipefail

WEB_ROOT="/var/www/kirin-frontend"
RELEASES_DIR="${WEB_ROOT}/releases"
CURRENT_LINK="${WEB_ROOT}/current"
LOCK_FILE="${WEB_ROOT}/deploy.lock"
KEEP_RELEASES=5

INCOMING_DIR="${1:-}"
RELEASE_ID="${2:-}"

log() { echo "[deploy] $*"; }

fail() {
  echo "RESULT=$1"
  exit 1
}

mkdir -p "$RELEASES_DIR"

exec 200>"$LOCK_FILE"
if ! flock -n 200; then
  log "Another deploy is already in progress, aborting."
  fail "precondition_failed"
fi

if [ -z "$INCOMING_DIR" ] || [ -z "$RELEASE_ID" ]; then
  log "Usage: deploy.sh <incoming-dist-dir> <release-id>"
  fail "precondition_failed"
fi

if [ ! -f "${INCOMING_DIR}/index.html" ]; then
  log "Refusing to deploy: ${INCOMING_DIR}/index.html does not exist (empty or broken build)."
  fail "precondition_failed"
fi

NEW_RELEASE_DIR="${RELEASES_DIR}/${RELEASE_ID}"
PREV_TARGET=""
if [ -L "$CURRENT_LINK" ]; then
  PREV_TARGET=$(readlink -f "$CURRENT_LINK")
fi

log "Installing new release at ${NEW_RELEASE_DIR}"
rm -rf "$NEW_RELEASE_DIR"
mkdir -p "$NEW_RELEASE_DIR"
cp -a "${INCOMING_DIR}/." "$NEW_RELEASE_DIR/"

swap_to() {
  ln -sfn "$1" "$CURRENT_LINK"
}

smoke_test() {
  local ok=true
  if ! curl -fsS --max-time 5 http://127.0.0.1/ | grep -qi '<div id="root"'; then
    log "Smoke test failed: homepage did not return expected content."
    ok=false
  fi
  if ! curl -fsS --max-time 5 http://127.0.0.1/api/actuator/health | grep -q '"status":"UP"'; then
    log "Smoke test failed: /api/actuator/health did not report UP through nginx."
    ok=false
  fi
  [ "$ok" = "true" ]
}

log "Swapping ${CURRENT_LINK} -> ${NEW_RELEASE_DIR}"
swap_to "$NEW_RELEASE_DIR"

if smoke_test; then
  log "Smoke tests passed."
  find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d ! -name "$RELEASE_ID" -printf '%T@ %p\n' 2>/dev/null \
    | sort -rn | tail -n +$((KEEP_RELEASES)) | cut -d' ' -f2- | xargs -r rm -rf
  rm -rf "$INCOMING_DIR"
  echo "RESULT=success"
  exit 0
fi

log "New release failed smoke tests, rolling back."
if [ -z "$PREV_TARGET" ] || [ ! -d "$PREV_TARGET" ]; then
  log "No previous release available to roll back to."
  fail "no_rollback_target"
fi

if ! swap_to "$PREV_TARGET"; then
  fail "rollback_failed"
fi

if smoke_test; then
  log "Rollback successful, previous release is serving again."
  fail "rolled_back"
else
  log "Rollback swap succeeded but smoke tests still fail."
  fail "rollback_unverified"
fi
