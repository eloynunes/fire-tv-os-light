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
- Build output is unsigned. Existing helper scripts may assume an already-signed APK and a particular macOS SDK layout; follow README build/sign instructions instead of treating those scripts as portable installers.

## Fixed in 0.2.0

Settings are no longer classified as Home merely because they share Amazon's package. The Settings button uses Amazon's actual main-settings entry point instead of the generic action routed to a CTS handler. Pending Home/app launches and delayed curtain callbacks are cancelled at lifecycle boundaries. Automatic privileged actions were removed and automatic cleanup now defaults to off.
