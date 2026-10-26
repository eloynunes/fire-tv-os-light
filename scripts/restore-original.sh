#!/usr/bin/env bash
# Disable redirection only; external device tuning has separate rollback.
set -euo pipefail
case "${1:-}" in
  ""|--keep-app) ;;
  *) echo "Usage: $0 [--keep-app]. Use install.sh --uninstall explicitly to remove app data." >&2; exit 2 ;;
esac
"$(dirname "$0")/home-lock.sh" off
echo "TV Light redirection disabled. App and external settings preserved."
