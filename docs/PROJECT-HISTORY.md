# What was built and what was learned

## Project purpose

TV Light is a small native Kotlin launcher for a memory-constrained Fire TV. It aims to provide a simple favorites/app grid navigable with a remote, reduce unwanted Home redirection delays and avoid unnecessary background work. It is not a Fire OS replacement, a root installer or a general debloating recommendation.

## Development sequence

1. Implemented the app grid, persistent favorites/order/hidden apps, remote navigation, settings and app discovery/icon caching. JVM tests cover state and ordering behavior.
2. Investigated Home on Fire OS 7.7.1.7. The Amazon launcher cannot simply be disabled with the normal shell privileges available on the reference firmware. A reported Success from set-home-activity did not establish an effective replacement. Accessibility redirection and a temporary full-screen curtain were used instead.
3. Investigated memory use. Prime Video was observed resident in the background and reportedly associated with an Amazon Home service binding. This was an observation on one firmware, not a universal or independently benchmarked guarantee. killBackgroundProcesses is not equivalent to force-stop; process death, available RAM and improved performance must be measured separately.
4. Investigated a third-party GhostLock kernel-root method. Device properties selected the declared profile and a kernel-configuration hash matched, but these checks did not establish identical kernel binaries or validate all offsets. One trial failed before confirming root with `fops marker absent ... holding reclaim`. The device was normally rebooted and no root was confirmed. No exploit is included in this repository. Do not repeat historical instructions as an installation step.
5. Reviewed the third-party root architecture: temporary root, no automatic Magisk installation, attempted permissive SELinux, local shell services without per-app approval and OTA changes. This did not meet the project's daily-use safety goals. Automatic su detection and privileged actions were removed from TV Light.
6. Investigated a separate external, manually controlled ADB companion. The reported research progressed from whole-device media checks to package/UID-specific observation, with simulated tests. Real idle observations remained indeterminate when Prime had no explicit stopped session. These experimental companions are not shipped or endorsed here; this repo contains neither a validated playback-aware stop service nor an app-integrated replacement.
7. Version 0.2.0 corrected lifecycle/callback handling and pending launches. Amazon Settings and Home share a package: only recognized Home Activity classes now trigger redirection. The settings button uses Amazon's main-settings entry instead of the generic Android action, which was routed through a CTS dummy handler on the reference firmware.
8. Validated Settings access, navigation to a subpage, Home return, Netflix remaining foreground in a short observation and behavior after reboot. Automatic cleanup remained disabled; SELinux remained Enforcing. App data was preserved through compatible signed updates.
9. Hardened repository tooling: no implicit device address, preservation of unrelated accessibility services, readback checks, signed-package validation, error propagation, non-destructive restore defaults and explicit opt-in for legacy memory/agent utilities. Release signing can use owner-provided environment credentials; private keys are never committed.

## Validation and its limits

The 0.2.0 application passed 42 JVM tests. Release lint had zero errors and 20 warnings. The later tooling changes passed 13 isolated Python tests, including fake-ADB installer success/failure cases; signed and unsigned release build paths were checked. Test suites differ: do not add unrelated research-companion counts to the app count.

Manual device checks cover one sheldon/AFTSS Fire OS build. They do not certify every event ordering, long-duration use, all streaming apps, DRM, all remotes, all firmware or flawless Home animation. The repository does not promise a fixed RAM saving or startup-time improvement.

## Current boundaries

The supported baseline is no root, no OTA blocking and SELinux Enforcing. Cleanup is off by default. There is no privileged shell server, automatic Prime termination or exploit bundled in the app. Accessibility remains opt-in and can draw an overlay/launch Activities; review SECURITY.md.

See OPEN-PROBLEMS.md for concrete contribution opportunities. Portuguese documents are retained as marked historical notes rather than authoritative operational guidance.
