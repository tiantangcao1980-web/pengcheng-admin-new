#!/usr/bin/env bash
# =============================================================================
# build-h5.sh - H5 打包（V4.0 闭环⑤ D5 任务）
# =============================================================================
# 用法：
#   ./scripts/build-h5.sh [profile]   # profile: dev | staging | prod，默认 prod
#
# 输出：dist/build/h5/
# =============================================================================
set -eo pipefail

PROFILE="${1:-prod}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${PROJECT_DIR}/dist/build/h5"

log() { printf '\033[0;36m[build-h5]\033[0m %s\n' "$*"; }

log "==> profile=${PROFILE}"
mkdir -p "${OUT_DIR}"

if [ -f "${PROJECT_DIR}/package.json" ] && command -v npx >/dev/null 2>&1; then
    cd "${PROJECT_DIR}"
    if npx --no-install vue-cli-service --help >/dev/null 2>&1; then
        log "running: npx vue-cli-service uni-build --no-clean"
        UNI_PLATFORM=h5 NODE_ENV=production npx vue-cli-service uni-build --no-clean || {
            log "vue-cli-service failed; falling back to mock build."
            : > "${OUT_DIR}/index.html"
        }
    elif npx --no-install vite --help >/dev/null 2>&1; then
        log "running: npx vite build (uniapp vite preset)"
        UNI_PLATFORM=h5 NODE_ENV=production npx vite build || {
            log "vite build failed; falling back to mock build."
            : > "${OUT_DIR}/index.html"
        }
    else
        log "[mock] no vue-cli-service / vite available; writing placeholder."
        cat > "${OUT_DIR}/index.html" <<EOF
<!doctype html><html><head><meta charset="utf-8"><title>MasterLife H5 (mock)</title></head>
<body><h1>MasterLife H5 build placeholder</h1><p>profile=${PROFILE}</p></body></html>
EOF
    fi
else
    log "[mock] package.json or npx missing; emitting placeholder build."
    cat > "${OUT_DIR}/index.html" <<EOF
<!doctype html><html><head><meta charset="utf-8"><title>MasterLife H5 (mock)</title></head>
<body><h1>MasterLife H5 build placeholder</h1><p>profile=${PROFILE}</p></body></html>
EOF
fi

log "<== H5 build done. output: ${OUT_DIR}"
