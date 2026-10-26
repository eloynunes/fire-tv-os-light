# Known issues — TV Light 0.2.0

- Amazon Home can flash before accessibility redirection; native Home replacement is not implemented.
- Compatibility is tested only on the sheldon/AFTSS firmware listed in the README. Amazon firmware changes may alter accessibility events or Settings components.
- The Home allowlist recognizes known Amazon Home Activity names. Unrecognized windows are not force-redirected; firmware changes may require an update.
- App launches are asynchronous. Cancelling a pending request cannot undo an Activity launch already accepted by Android.
- No validated automatic Prime Video termination, playback-aware cleanup, privileged backend or root support.
- Release lint retains 20 warnings (including newer Android API behavior, vector compatibility and Context/view patterns). Zero lint errors does not mean zero runtime bugs. Warnings were not suppressed to claim a clean audit.
- Short device checks do not validate all streaming services, DRM, physical remote timing, long-term memory behavior or every lifecycle sequence.
- APK rollback can be rejected by Android's downgrade/signature rules; backup APKs alone do not preserve private data.
- UI strings and historical developer notes are currently Portuguese. The public overview and security policy are English; full UI localization is not yet implemented.
- Build output is unsigned. Installation now requires a pre-signed TV Light APK, verifies signature integrity and package identity, and stops on errors. SDK discovery still defaults to a macOS Homebrew layout; set environment paths on other systems.

## Fixed in 0.2.0

Settings are no longer classified as Home merely because they share Amazon's package. The Settings button uses Amazon's actual main-settings entry point instead of the generic action routed to a CTS handler. Pending Home/app launches and delayed curtain callbacks are cancelled at lifecycle boundaries. Automatic privileged actions were removed and automatic cleanup now defaults to off.

## Repository hardening after 0.2.0

Accessibility updates preserve other services and include readback verification. Install no longer masks ADB failures or re-enables accessibility blindly; it rejects unsigned APKs and other package names. Device selection is explicit. Restore no longer uninstalls or resets unrelated settings. Release signing supports owner-provided environment credentials without embedding keys. These tooling/build changes do not replace the installed app certificate or claim to eliminate the Home flash.
