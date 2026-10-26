# TV Light

A lightweight Android TV launcher for Amazon Fire TV, built with Kotlin and native Android views. Current version: **0.2.0** (versionCode 4).

TV Light provides favorites, app ordering, hidden apps, remote-control navigation and an accessibility-based Home redirect. It does not replace Fire OS or install root.

## Support and limitations

Device-tested on Fire TV `sheldon` / `AFTSS`, Fire OS 7.7.1.7 (`PS7717.5741N`, incremental `0036408028548`), Android 9. Other devices and firmware versions are not validated.

- Home redirection uses an optional accessibility service. The Amazon Home may briefly appear; this is not a native Home replacement.
- Fire TV Settings open through Amazon's settings entry point. Home and Settings windows are distinguished so Settings are not redirected.
- Automatic memory cleanup is disabled by default. Non-root cleanup requests do not guarantee that an app exits or that memory/performance improves.
- Automatic Prime Video force-stop and privileged Home configuration are not implemented.
- No `su` probing, kernel exploit, network permission or privileged backend is included in the app.
- Security review is limited: this project is not a certified secure product. Read [SECURITY.md](SECURITY.md) and [known issues](docs/KNOWN-ISSUES.md).

## Build

Requires JDK 17 and Android SDK 34. Set `sdk.dir` in your untracked `local.properties` or configure your Android SDK environment.

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease
```

Release builds are unsigned by default. For owner-controlled signing, set all four environment variables: `TVLIGHT_KEYSTORE` (absolute path), `TVLIGHT_STORE_PASSWORD`, `TVLIGHT_KEY_ALIAS`, and `TVLIGHT_KEY_PASSWORD`. Never commit these values or a keystore. If configured incompletely, the build fails instead of choosing a fallback key.

Developer scripts now require an explicit `DEVICE` ADB serial or IP:port. `APK=/absolute/signed.apk DEVICE=serial ./scripts/install.sh` verifies the APK signature and package name, then requests an update without automatic uninstall or accessibility changes. Set `APKSIGNER`/`AAPT` if build-tools are not discovered. `home-lock.sh on|off` preserves unrelated accessibility services. `restore-original.sh` now only disables TV Light redirection; it does not reset unrelated settings or uninstall the app. Uninstall remains explicit and destroys app data.

 Sign your APK with a key you own and keep it secure. Android updates require a compatible signing certificate. Do not uninstall to bypass a certificate mismatch if you need to preserve app data.

The privately tested APK used the existing local Android debug certificate for update compatibility. That certificate is not a production identity; no private signing key is distributed here.

## Install and use

Install a signed APK using Android's package installer or authorized ADB on your own device. Keep ADB on a trusted network and disable debugging when finished. Enable the TV Light accessibility service manually in Fire TV accessibility settings if you want Home redirection; the launcher itself can run without it.

Remote controls: directional buttons navigate, OK opens an app, Menu or long-press OK opens options. In Move mode, directions reorder and OK confirms. Settings provides an explicit route to Amazon Home.

Updates preserve data when accepted by Android. Uninstalling does not undo external ADB settings, disabled packages or computer agents.

## Validation

The 0.2.0 build passed 42 JVM tests and release lint with zero errors and 20 documented warnings. On the reference device, installation, the Settings button, Settings navigation, Home return, Netflix staying foreground during a short observation, and behavior after reboot were checked. These tests do not establish universal compatibility, DRM certification, long-term stability or absence of security bugs.

## Repository layout

- `app/`: Android app and JVM tests.
- `scripts/`: optional developer/device utilities; some legacy tools change device settings or create computer agents. Do not run them as a setup checklist.
- `docs/KNOWN-ISSUES.md`: current limitations and risks.
- Other Portuguese documents under `docs/`: historical development notes, not current installation instructions. Security-sensitive claims are superseded by this README and SECURITY.md.

## Contributing and licensing

Include device/build information and reproducible steps in reports; redact account information and media titles. Never attach signing keys, ADB keys, credentials or complete private device dumps. No license has been selected yet: public visibility alone does not grant an open-source license. Obtain permission before redistribution or reuse beyond applicable law.
