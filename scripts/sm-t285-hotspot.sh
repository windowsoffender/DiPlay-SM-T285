#!/usr/bin/env bash
# Controls the rooted SM-T285's Wi-Fi hotspot over adb, without a SIM.
# Samsung Settings won't start the hotspot without a SIM, but the Wi-Fi service itself doesn't care.
# For everyday use DiPlay starts the hotspot itself: Connection > Built-in car hotspot, then
# "Automatically turn on the car hotspot". This script is for setting the hotspot up.
#
#   scripts/sm-t285-hotspot.sh on|off|status
#   scripts/sm-t285-hotspot.sh name <name>    rename it (hotspot off); also change it in DiPlay
#   scripts/sm-t285-hotspot.sh channel [n]    show or pin the channel (hotspot off)
#
# Keep a unique name: an iPhone that knows another "AndroidAP" won't join this one.
# Keep a fixed channel: DiPlay tells the iPhone the channel, and "auto" (0) can't be read back.
#
# It doesn't survive a reboot. Don't turn it into a Magisk boot script: on this tablet the
# kernel blocks the processes those scripts start, the wait loop spins, and the tablet runs out
# of memory and reboots about 90 seconds after every boot.
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
build_tools=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)
java_home=${JAVA_HOME:-/opt/android-studio/jbr}
dex=/data/local/tmp/diplay-hotspot.dex

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
"$java_home/bin/javac" --release 8 -nowarn -d "$work" "$here/sm-t285-hotspot/Hotspot.java" 2>&1 | { grep -v '^warning' || true; }
"${build_tools}d8" --min-api 22 --output "$work" "$work/Hotspot.class"
adb push "$work/classes.dex" $dex >/dev/null 2>&1

adb shell "su -c 'CLASSPATH=$dex app_process /system/bin Hotspot ${1:-status}'" 2>&1 | { grep -v '^WARNING: linker' || true; }
