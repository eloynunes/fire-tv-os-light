#!/usr/bin/env bash
# Change only TV Light; preserve unrelated accessibility services.
set -euo pipefail
source "$(dirname "$0")/env.sh"
: "${DEVICE:?Set DEVICE to your explicitly authorized ADB serial or IP:port}"
exec python3 "$(dirname "$0")/accessibility.py" "${1:-status}" --serial "$DEVICE"
