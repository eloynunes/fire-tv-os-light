# Ambiente comum dos scripts (source).
export JAVA_HOME="${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS="$ROOT/testing/results"
PKG="com.firetvoslight.launcher"
DEVICE="${DEVICE:-}"
APK="${APK:-$ROOT/app/build/outputs/apk/release/app-release.apk}"
mkdir -p "$RESULTS"
ADB="${ADB:-adb}"
TS="$(date +%Y%m%d-%H%M%S)"
adbd() { : "${DEVICE:?Set DEVICE to your authorized ADB serial or IP:port}"; "$ADB" -s "$DEVICE" "$@"; }
