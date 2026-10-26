#!/usr/bin/env bash
# Install a pre-signed APK; never uninstall to bypass an update failure.
set -euo pipefail
source "$(dirname "$0")/env.sh"
: "${DEVICE:?Set DEVICE to your authorized ADB serial or IP:port}"
if [[ "${1:-}" == "--uninstall" ]]; then
  "$(dirname "$0")/home-lock.sh" off
  adbd uninstall "$PKG"
  exit 0
fi
[[ -z "${1:-}" ]] || { echo "Usage: APK=/absolute/signed.apk DEVICE=serial $0 [--uninstall]" >&2; exit 2; }
[[ -f "$APK" ]] || { echo "Set APK to a pre-signed APK; unsigned releases are not installed automatically." >&2; exit 2; }
if [[ -z "${APKSIGNER:-}" ]]; then
  for candidate in "$ANDROID_HOME"/build-tools/*/apksigner; do
    [[ ! -x "$candidate" ]] || APKSIGNER="$candidate"
  done
fi
: "${APKSIGNER:?Set APKSIGNER to Android build-tools apksigner}"
"$APKSIGNER" verify "$APK"
AAPT="${AAPT:-$(dirname "$APKSIGNER")/aapt}"
badging=$("$AAPT" dump badging "$APK")
package_line=${badging%%$'\n'*}
[[ "$package_line" == "package: name='$PKG' "* ]] || { echo "APK is not TV Light; refusing installation." >&2; exit 2; }
# Android enforces signatures/version compatibility. On failure, stop immediately.
adbd install -r "$APK"
echo "Update accepted. Check accessibility manually; no service list was overwritten."
