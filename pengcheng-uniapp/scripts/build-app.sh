#!/usr/bin/env bash
# =============================================================================
# build-app.sh - APP 离线打包流水线（V4.0 闭环⑤ D5 任务）
#
# 用法：
#   ./scripts/build-app.sh android <channel>     # Android 多渠道打包
#   ./scripts/build-app.sh ios     <profile>     # iOS 企业签 / TestFlight
#   ./scripts/build-app.sh all     release       # 同时出 Android + iOS
#
# 渠道占位：huawei / xiaomi / oppo / vivo / appstore / dev / default
#
# 设计原则：
#   - 不强制依赖 HBuilderX；如果环境变量 HBX_CLI 指向真实 cli，则调用之；
#     否则走 mock 模式（只 echo 命令，便于 CI 干跑）。
#   - 渠道号通过 build.json 注入（替换 ${CHANNEL} 占位）。
#   - 输出目录：dist/build/app-plus/<platform>/<channel>/
# =============================================================================

set -eo pipefail

PLATFORM="${1:-android}"
CHANNEL="${2:-default}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${PROJECT_DIR}/dist/build/app-plus/${PLATFORM}/${CHANNEL}"

# HBuilderX CLI 路径（用户在 CI 中通过环境变量注入）
HBX_CLI="${HBX_CLI:-mock-hbx}"

log() { printf '\033[0;36m[build-app]\033[0m %s\n' "$*"; }
err() { printf '\033[0;31m[build-app][ERR]\033[0m %s\n' "$*" >&2; }

# -----------------------------------------------------------------------------
# 渠道号注入：CLI 调用前向 manifest.json 写入临时副本
# -----------------------------------------------------------------------------
inject_channel() {
    local channel="$1"
    local manifest="${PROJECT_DIR}/manifest.json"
    if [ ! -f "$manifest" ]; then
        err "manifest.json not found: ${manifest}"
        exit 1
    fi
    log "inject channel=${channel} into manifest.json (logical)"
    # 实际生产建议用 jq；此处保持 mock-friendly
    if command -v jq >/dev/null 2>&1; then
        jq --arg ch "${channel}" '.["app-plus"].distribute.android.channel = $ch' \
            "$manifest" > "${manifest}.tmp" && mv "${manifest}.tmp" "$manifest"
    else
        log "jq not available, skipping in-place channel write (CI mock OK)"
    fi
}

# -----------------------------------------------------------------------------
# Android 打包
# -----------------------------------------------------------------------------
build_android() {
    local channel="$1"
    log "==> Android build start, channel=${channel}, output=${OUT_DIR}"
    mkdir -p "${OUT_DIR}"
    inject_channel "${channel}"

    if command -v "${HBX_CLI}" >/dev/null 2>&1 && [ "${HBX_CLI}" != "mock-hbx" ]; then
        "${HBX_CLI}" publish \
            --platform android \
            --project "${PROJECT_DIR}" \
            --output "${OUT_DIR}" \
            --channel "${channel}"
    else
        log "[mock] HBX_CLI not configured. Would run:"
        log "  ${HBX_CLI} publish --platform android --project ${PROJECT_DIR} --output ${OUT_DIR} --channel ${channel}"
        # 干跑写一个占位 APK 描述符，便于 CI 流水线 verify
        cat > "${OUT_DIR}/build-info.json" <<EOF
{
  "platform": "android",
  "channel": "${channel}",
  "manifestPath": "${PROJECT_DIR}/manifest.json",
  "mock": true,
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
    fi
    log "<== Android build done"
}

# -----------------------------------------------------------------------------
# iOS 打包（企业签 / TestFlight）
# -----------------------------------------------------------------------------
build_ios() {
    local profile="$1"
    log "==> iOS build start, profile=${profile}, output=${OUT_DIR}"
    mkdir -p "${OUT_DIR}"

    # iOS 需要证书与 provisioning profile，这里仅占位
    : "${IOS_CERT_PATH:=}"            # .p12 证书路径
    : "${IOS_CERT_PASSWORD:=}"        # 证书密码（CI Secret）
    : "${IOS_PROVISION_PATH:=}"       # .mobileprovision 路径
    : "${IOS_TEAM_ID:=}"

    if [ -z "${IOS_CERT_PATH}" ] || [ -z "${IOS_PROVISION_PATH}" ]; then
        log "[mock] iOS 证书未配置（IOS_CERT_PATH / IOS_PROVISION_PATH）。"
        log "       仅生成 build-info.json 占位，请在 CI Secrets 中补齐后重跑。"
        cat > "${OUT_DIR}/build-info.json" <<EOF
{
  "platform": "ios",
  "profile": "${profile}",
  "mock": true,
  "missing": ["IOS_CERT_PATH", "IOS_PROVISION_PATH"],
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
        return
    fi

    if command -v "${HBX_CLI}" >/dev/null 2>&1 && [ "${HBX_CLI}" != "mock-hbx" ]; then
        "${HBX_CLI}" publish \
            --platform ios \
            --project "${PROJECT_DIR}" \
            --output "${OUT_DIR}" \
            --profile "${profile}" \
            --cert "${IOS_CERT_PATH}" \
            --provision "${IOS_PROVISION_PATH}"
    else
        log "[mock] would run iOS publish with profile=${profile}"
    fi
    log "<== iOS build done"
}

# -----------------------------------------------------------------------------
# main
# -----------------------------------------------------------------------------
case "${PLATFORM}" in
    android)
        build_android "${CHANNEL}"
        ;;
    ios)
        build_ios "${CHANNEL}"
        ;;
    all)
        for ch in huawei xiaomi oppo vivo default; do
            build_android "${ch}"
        done
        build_ios "appstore"
        ;;
    *)
        err "unknown platform: ${PLATFORM} (expect android|ios|all)"
        exit 1
        ;;
esac

log "DONE."
