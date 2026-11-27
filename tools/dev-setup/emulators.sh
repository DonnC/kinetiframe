#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Creates and starts the emulators KinetiFrame is checked on: small phone, phone, foldable, 7-inch
# and 11-inch tablet at the target API, plus one phone at the oldest supported API.
#
#   tools/dev-setup/emulators.sh install    # download system images (once)
#   tools/dev-setup/emulators.sh create     # make the AVDs (skips any that exist)
#   tools/dev-setup/emulators.sh list       # show KinetiFrame AVDs and running devices
#   tools/dev-setup/emulators.sh start kf-tablet [kf-phone ...]
#   tools/dev-setup/emulators.sh install-app   # build once, install on every running device
#
# Works on macOS (Apple silicon or Intel) and Linux. Set ANDROID_HOME if the SDK isn't in the
# default place, KF_API to use another API level.
set -euo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[ -d "$SDK" ] || SDK="$HOME/Android/Sdk"
[ -d "$SDK" ] || { echo "Android SDK not found. Set ANDROID_HOME." >&2; exit 1; }

TOOLS="$SDK/cmdline-tools/latest/bin"
[ -x "$TOOLS/sdkmanager" ] || {
  echo "Install 'Android SDK Command-line Tools (latest)' in Android Studio › Settings › SDK Tools." >&2
  exit 1
}
SDKMANAGER="$TOOLS/sdkmanager"
AVDMANAGER="$TOOLS/avdmanager"
EMULATOR="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"

# KinetiFrameBuild.TARGET_SDK. The SDK names this image android-37.0, not android-37.
API="${KF_API:-37.0}"
MIN_API=28            # KinetiFrameBuild.MIN_SDK
case "$(uname -m)" in arm64|aarch64) ABI=arm64-v8a ;; *) ABI=x86_64 ;; esac

image() { echo "system-images;android-$1;google_apis;$ABI"; }

# name|device profile|api. Profiles come from `avdmanager list device -c`.
PROFILES=(
  "kf-phone-small|small_phone|$API"
  "kf-phone|pixel_8|$API"
  "kf-foldable|pixel_9_pro_fold|$API"
  "kf-tablet-small|small_tablet|$API"
  "kf-tablet|pixel_tablet|$API"
  "kf-phone-oldest|pixel_4|$MIN_API"
)

install() {
  yes | "$SDKMANAGER" --licenses >/dev/null || true
  "$SDKMANAGER" "platform-tools" "emulator" "$(image "$API")"
  # Check the image landed: given a wrong name, sdkmanager just prints "not found".
  [ -d "$SDK/system-images/android-$API/google_apis/$ABI" ] || {
    echo "$(image "$API") didn't install. Find the right name with: $SDKMANAGER --list | grep system-images" >&2
    echo "then run again with KF_API set to its android-<level> part." >&2
    exit 1
  }
  # The oldest API may have no image for this CPU; that phone is then skipped.
  "$SDKMANAGER" "$(image "$MIN_API")" || true
  [ -d "$SDK/system-images/android-$MIN_API/google_apis/$ABI" ] ||
    echo "No $(image "$MIN_API"); kf-phone-oldest will be skipped."
}

create() {
  local devices existing
  devices="$("$AVDMANAGER" list device -c)"
  existing="$("$EMULATOR" -list-avds || true)"
  for entry in "${PROFILES[@]}"; do
    IFS='|' read -r name device api <<<"$entry"
    if grep -qx "$name" <<<"$existing"; then echo "exists: $name"; continue; fi
    if ! grep -qx "$device" <<<"$devices"; then
      echo "skip $name: this SDK has no '$device' profile (update the emulator or pick one from 'avdmanager list device -c')"
      continue
    fi
    if [ ! -d "$SDK/system-images/android-$api/google_apis/$ABI" ]; then
      echo "skip $name: system image for API $api not installed (run: $0 install)"
      continue
    fi
    echo no | "$AVDMANAGER" create avd -n "$name" -k "$(image "$api")" -d "$device" >/dev/null
    # A roomier disk and RAM keep large canvases and exports from failing on the emulator.
    cfg="$HOME/.android/avd/$name.avd/config.ini"
    { echo "disk.dataPartition.size=8G"; echo "hw.ramSize=4096"; echo "hw.keyboard=yes"; } >>"$cfg"
    echo "created: $name ($device, API $api)"
  done
}

list() {
  echo "KinetiFrame AVDs:"; "$EMULATOR" -list-avds | grep '^kf-' || echo "  none (run: $0 create)"
  echo; "$ADB" devices -l
}

start() {
  [ $# -gt 0 ] || { echo "usage: $0 start <avd> [<avd> ...]" >&2; exit 1; }
  for name in "$@"; do
    nohup "$EMULATOR" -avd "$name" -no-snapshot-save -no-boot-anim >"/tmp/$name.log" 2>&1 &
    echo "starting $name (log: /tmp/$name.log)"
  done
  # `adb wait-for-device` fails once more than one device is attached, so count them instead.
  until [ "$("$ADB" devices | awk 'NR>1 && $2=="device"' | wc -l)" -ge $# ]; do sleep 2; done
  echo "Running devices:"; "$ADB" devices -l
}

install_app() {
  local root apk
  root="$(cd "$(dirname "$0")/../.." && pwd)"
  (cd "$root" && ./gradlew :app:assembleDebug -q)
  apk="$root/app/build/outputs/apk/debug/app-debug.apk"
  for serial in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
    "$ADB" -s "$serial" shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'
    echo "installing on $serial"; "$ADB" -s "$serial" install -r "$apk" >/dev/null
  done
}

case "${1:-}" in
  install) install ;;
  create) create ;;
  list) list ;;
  start) shift; start "$@" ;;
  install-app) install_app ;;
  *) sed -n '4,13p' "$0"; exit 1 ;;
esac
