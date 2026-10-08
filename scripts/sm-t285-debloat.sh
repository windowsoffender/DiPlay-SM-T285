#!/usr/bin/env bash
# Strips a rooted SM-T285 (Galaxy Tab A 2016, Android 5.1.1) down to a DiPlay-only tablet.
#
#   scripts/sm-t285-debloat.sh           disable the apps below and apply the car tweaks
#   scripts/sm-t285-debloat.sh restore   turn every app below back on
#
# Apps are disabled, not deleted. Disabled apps use no RAM and never start, and they still show
# under Settings > Applications > Disabled, so any of them can come back without a computer.
# Install the DiPlay build that is also the home screen first, or the Samsung launcher stays on.
set -euo pipefail

DIPLAY=com.shihab.diplay.hudtest
LAUNCHER=com.sec.android.app.launcher

APPS=(
  # Google. No account is signed in and apps go on with adb.
  com.android.vending
  com.google.android.gms
  com.google.android.gsf
  com.google.android.gsf.login
  com.google.android.backuptransport
  com.google.android.configupdater
  com.google.android.feedback
  com.google.android.onetimeinitializer
  com.google.android.partnersetup
  com.google.android.setupwizard
  com.google.android.syncadapters.calendar
  com.google.android.syncadapters.contacts
  com.google.android.androidforwork
  com.google.android.gm
  com.google.android.apps.maps
  com.google.android.apps.docs
  com.google.android.youtube
  com.google.android.tts
  com.google.android.marvin.talkback
  com.android.chrome
  com.sec.android.app.chromecustomizations
  com.android.browser.provider
  com.android.providers.partnerbookmarks

  # Everyday apps and widgets
  com.android.calendar
  com.android.contacts
  com.android.mms
  com.samsung.android.app.memo
  com.samsung.android.email.composer
  com.samsung.android.email.provider
  com.samsung.android.email.sync
  com.samsung.android.email.ui
  com.samsung.android.email.widget
  com.samsung.android.video
  com.sec.android.app.camera
  com.sec.android.app.clockpackage
  com.sec.android.app.fm
  com.sec.android.app.music
  com.sec.android.app.myfiles
  com.sec.android.app.popupcalculator
  com.sec.android.app.sbrowser
  com.sec.android.gallery3d
  com.sec.android.providers.tasks
  com.sec.android.daemonapp
  com.sec.android.widgetapp.at.hero.accuweather
  com.sec.android.widgetapp.digitalclock
  com.sec.android.widgetapp.dualclockdigital
  com.sec.android.widgetapp.webmanual
  com.samsung.android.widgetapp.connectivitywidget
  com.sec.android.provider.badge
  com.sec.android.pagebuddynotisvc
  com.sec.android.app.snsimagecache

  # Look and feel extras
  com.android.dreams.basic
  com.android.dreams.phototable
  com.android.wallpapercropper
  com.android.wallpaper.livepicker
  com.sec.android.app.wallpaperchooser
  com.sec.android.wallpapercropper2
  com.sec.android.app.personalization
  com.monotype.android.font.cooljazz
  com.monotype.android.font.foundation
  com.sec.android.app.FlashBarService
  com.visionobjects.resourcemanager
  com.samsung.SMT
  com.samsung.android.app.accesscontrol
  com.samsung.android.app.assistantmenu
  com.samsung.android.app.colorblind
  com.android.htmlviewer
  com.android.printspooler
  com.sec.android.app.ringtoneBR

  # Samsung file sharing over Wi-Fi Direct, which DiPlay uses for wireless CarPlay
  com.samsung.android.allshare.service.fileshare
  com.samsung.android.app.FileShareClient
  com.samsung.android.app.FileShareServer

  # Setup, updates, cloud, Knox and tracking
  com.sec.android.app.SecSetupWizard
  com.android.managedprovisioning
  com.sec.android.preloadinstaller
  com.sec.android.AutoPreconfig
  com.sec.android.Preconfig
  com.sec.android.Kies
  com.policydm
  com.wssyncmldm
  com.wsomacp
  com.wssnps
  com.sec.android.soagent
  com.sec.android.diagmonagent
  com.samsung.android.scloud.auth
  com.samsung.android.scloud.backup
  com.samsung.android.scloud.sync
  com.samsung.android.fmm
  com.samsung.android.mdm
  com.samsung.android.securitylogagent
  com.samsung.klmsagent
  com.sec.esdk.elm
  com.sec.enterprise.knox.cloudmdm.smdms
  com.samsung.aasaservice
  com.samsung.safetyinformation
  com.sec.android.app.mt
  com.sec.android.app.sysscope
  com.cleanmaster.sdk
  com.samsung.android.sm
  com.samsung.android.smcore
  org.simalliance.openmobileapi.service
  com.offsec.nethunter.store.privileged

  # Factory test tools
  com.sec.android.app.DataCreate
  com.sec.android.app.factorykeystring
  com.sec.android.app.hwmoduletest
  com.sec.android.app.wlantest
  com.sec.android.app.bluetoothtest
  com.sec.factory.camera
  com.sec.android.app.parser
  com.sec.tcpdumpservice
)
# Kept on purpose: the phone service, SIM toolkit, VoLTE and in-call screen (the hotspot needs a SIM),
# Bluetooth, VPN dialogs, location, the Samsung keyboard, USB/MTP, sound effects, emergency mode,
# Settings, SystemUI, WebView and every content provider the system reads.

root() { adb shell "su -c '$1'" 2>&1 | { grep -v '^WARNING: linker' || true; }; }

# One root shell for the whole list; pm prints "new state" for each app it changed.
run_all() {
  local action=$1 list script=/data/local/tmp/debloat.sh
  list=$(mktemp)
  for app in "${APPS[@]}"; do
    echo "pm $action $app 2>&1 | grep -q 'new state' || echo 'skipped $app'"
  done > "$list"
  adb push "$list" $script >/dev/null 2>&1
  rm "$list"
  root "sh $script; rm $script"
}

if [[ ${1:-} == restore ]]; then
  run_all enable
  root "pm enable $LAUNCHER" >/dev/null
  echo "All apps are back on. Reboot the tablet to finish."
  exit 0
fi

run_all disable

# The Samsung launcher only goes once DiPlay answers the home button.
root "pm disable $LAUNCHER" >/dev/null
if adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME | grep -q "$DIPLAY"; then
  echo "DiPlay is the home screen."
else
  root "pm enable $LAUNCHER" >/dev/null
  echo "This DiPlay build can't be the home screen, so the Samsung launcher stays."
fi

# Car tweaks: stay awake on any charger, and snappier animations.
adb shell settings put global stay_on_while_plugged_in 3
for scale in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb shell settings put global $scale 0.5
done

echo "Done. Reboot the tablet to finish."
