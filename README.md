# DiPlay on my old Samsung tablet

This is my fork of DiPlay. I use it to turn my old Galaxy Tab A 2016 (SM-T285, Android 5.1.1) into the CarPlay head unit for my car. The tablet runs nothing but DiPlay.

What I changed from the original:

- Runs on Android 5.1. The original needs Android 7.1 or newer
- DiPlay is the home screen, so the tablet boots straight into it
- DiPlay turns on the tablet's hotspot itself, no SIM needed. Samsung's settings won't turn on the hotspot without one
- It wakes up and connects when the car starts charging it, and disconnects and goes to sleep 30 seconds after the car turns off
- The BYD stuff stays off since my car isn't a BYD

## Setting up the tablet

The tablet is rooted with Magisk.

1. Build the APK with `DIPLAY_AUTH_ASSETS_DIR=$PWD/.private/runtime-assets ./gradlew :mobile:assembleStandaloneDebug` and install it with `adb install -r mobile/build/outputs/apk/debug/mobile-debug.apk`. The identity files aren't in the repo, see [Build a standalone car-test APK](docs/BUILD.md#build-a-standalone-car-test-apk)
2. Run `scripts/sm-t285-debloat.sh` to turn off everything DiPlay doesn't need. `scripts/sm-t285-debloat.sh restore` brings it all back
3. With the hotspot off, run `scripts/sm-t285-hotspot.sh name DiPlay-T285` and `scripts/sm-t285-hotspot.sh channel 6`. Give it a unique name, my iPhone wouldn't join "AndroidAP"
4. In DiPlay go to Settings > Connection > Open connection setup, pick Built-in car hotspot, enter the hotspot's name and password and turn on "Automatically turn on the car hotspot"
5. Under Settings > Connection > Automatic connection turn on "Sleep and wake with the car"
6. On the tablet set Lock screen > Screen lock type to None, or you'll have to swipe every time the car starts

Don't add Magisk boot scripts on this tablet. Samsung's kernel blocks them and it bootloops.

Everything below is the original DiPlay readme.

---

# DiPlay

**CarPlay for compatible BYD Android head units.** Wired and wireless, with the familiar DiAuto interface. Independent app: `com.shihab.diplay`.

> **BYD support scope:** These projects focus on BYD cars. They may work on other brands, but other brands are unsupported and there are no plans to add support or fix brand-specific incompatibilities.

[Download & website](https://shihabal3amri.github.io/DiPlay/) · [Release](https://github.com/shihabal3amri/DiPlay/releases/tag/v0.2.14) · [Report a problem](https://github.com/shihabal3amri/DiPlay/issues/new/choose)

![DiPlay home](site/assets/home.png)

## 0.2.14 — public preview

Install on the **car**, not the iPhone. No jailbreak, dongle, Mac, account or authentication server is required for use. Core CarPlay does not require ADB; optional dashboard, battery, wheel-speed and parked-video features do. Your head unit must permit APK installation. The APK supports Android 5.1+ (API 22); Android 5.1–8.1 support is new and not yet confirmed on a vehicle. Wireless supports Wi-Fi Direct, the car’s existing hotspot or Existing Wi-Fi / Same LAN. Android 9 Wi-Fi Direct uses a firmware-dependent legacy path with generated group credentials and unverified requested frequency; see [Android 9 Wi-Fi Direct](docs/ANDROID9_WIFI_DIRECT.md). Android 10+ verifies its negotiated group frequency.

- Wired USB and wireless CarPlay with local authentication.
- BYD HUD navigation with arrows, distance and street names on verified firmware.
- Car hotspot support, improved audio buffering and saved receive diagnostics.
- Automatic address discovery, fixed-channel Wi-Fi fallbacks and successful-configuration memory.
- Icon/text size, resolution and frame rate; applying a display change reconnects CarPlay.
- Local diagnostic export. Reports are sent only if you choose to share them.
- Separate installation alongside DiAuto. Run one projection app at a time.

This is **not an Apple-certified product**. The APK bundles an experimental accessory identity recovered from public Carlinkit firmware, not a newly provisioned MFi identity for DiPlay. A bundled private key is extractable. Acceptance after future iOS updates, reliability across head units and suitability of that identity for general distribution are unresolved. This release invites community testing; it is not a guarantee of universal compatibility.

Earlier releases were tested on the development DiLink5.1 car: live windshield guidance and street names work, Car hotspot now starts CarPlay, and Wi-Fi Direct performance is substantially improved. Occasional audio cutouts remain and are deferred to a later update. The floating-map test build was installed on the development DiLink 5.1 car; feedback led to the pinch corrections in 0.2.9. Earlier wheel-speed and video contributions were tested on a BYD Tang with DiLink 5.0 and an iPhone 15 Pro on iOS 27; wheel-speed dead reckoning in tunnels remains unverified. Broader head-unit and iOS compatibility is not guaranteed. The HUD firmware scope and cleanup limits are documented in [BYD navigation](docs/BYD_NAVIGATION.md).

## What’s new in 0.2.14

- Searchable Settings, clear categories, quick controls and reconnect notices, with layouts for short screens and Arabic RTL.
- **Interface size** from Automatic to 200% for DiPlay's own controls, separate from CarPlay picture sizing.
- Default automatic connection choice: Last used, Wireless or USB; scheduled day/night appearance; more reliable car-button image selection.
- Targeted USB/Bluetooth recovery, hotspot address discovery, video output on windows without hardware acceleration, and safer wireless handoff.
- **Smooth video (experimental)**, off by default, with a latency tradeoff and no picture adjustments on its SurfaceView path.
- Configurable wheel-key Siri, microphone-source fallback, optional audio-focus handling and BYD call-watcher repairs.
- **Call echo cancellation** and **Clearer call voices**, experimental and off by default; opt-in changes apply at the next connection.
- Album artwork proportions, system-bar-aware rotation/split-screen areas and recognition of an observed DiLink 3 cluster surface.

See [0.2.14 release notes](docs/RELEASE-NOTES-0.2.14.md) and [validation](docs/VALIDATION.md) for contribution links and remaining physical tests. General stutter, calls/Siri, decoder and model-specific reports still need current-device evidence. [0.2.13 notes](docs/RELEASE-NOTES-0.2.13.md) remain available as historical guidance.

If a problem remains, reproduce it on **0.2.14**, then use **Settings → Diagnostics → Save diagnostic report**. Android 10+ normally saves to **Downloads/DiPlay**; Android 9 uses the document picker. If unavailable, use **View report** or **Share** from the confirmation, which identifies external/private fallback storage. Review the `.txt` and add it to a matching [existing issue](https://github.com/shihabal3amri/DiPlay/issues), or [create one](https://github.com/shihabal3amri/DiPlay/issues/new/choose). Include vehicle/head-unit model, exact firmware and Android/DiLink, phone/iOS, connection backend, relevant settings, steps and failure time. Reports are shared only when you choose; never post your hotspot password.

## Documentation

[Existing Wi-Fi / Same LAN](docs/EXISTING_WIFI.md) keeps the iPhone and head unit
on an external router. See the guide for setup, build requirements and the
BYD DiLink 4.0 / Android 10 clean-install validation result.

- [Install and connect](docs/INSTALL.md)
- [Compatibility and troubleshooting](docs/COMPATIBILITY.md)
- [Smooth wireless CarPlay](docs/SMOOTH_WIRELESS.md)
- [Privacy and diagnostic reports](docs/PRIVACY.md)
- [Build from source](docs/BUILD.md) — select `mobile` for the main DiPlay app; `maphost` is a map sample.
- [Validation](docs/VALIDATION.md)
- [Release notes](CHANGELOG.md)
- [Credits and licenses](docs/THIRD_PARTY_NOTICES.md)

The app and release website are available in English, Arabic, Russian, Ukrainian, Spanish, Simplified Chinese and Traditional Chinese (Taiwan). Traditional Chinese uses Taiwan wording; the app also recognizes Hong Kong/Macao and explicit Hant selections without claiming separate regional translations. Choose the app language in Settings; on Android 13+, it stays synchronized with Android’s per-app language setting.

## Source and credits

Based on [xcertplay](https://github.com/shilapi/xcertplay), GPL-3.0. The home/settings UI and website adapt [DiAuto](https://github.com/shihabal3amri/DiAuto), AGPL-3.0; that license is included in `docs/licenses`. Preserve those notices when distributing modifications. CarPlay and its icon belong to Apple Inc.; no Apple or BYD affiliation or endorsement is implied.

This repository starts with a clean public source snapshot. Local research, tester reports and release-signing secrets are excluded. The complete source corresponding to the APK is provided with every release; experimental runtime identity assets are described separately in the build instructions and notices.

## Local release packaging

The release APK intentionally contains the experimental accessory identity. The Git repository and source archive exclude all accessory and Android signing keys; tests generate synthetic identities at runtime. Source/CI builds omit runtime identity assets by default. Local release builds explicitly select an external asset directory. Publishing the APK makes its bundled identity extractable; building locally does not preserve that identity's confidentiality.
