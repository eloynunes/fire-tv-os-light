# Help wanted: the two unresolved goals

These are open engineering problems, not advertised features. Please propose a reproducible hypothesis and provide evidence before changing device settings.

## 1. Eliminate the Amazon Home flash

**Current behavior:** pressing Home can expose Amazon Home before TV Light's accessibility curtain and Activity appear. Redirection is not native Home replacement. Delayed/already-delivered Android launches can race with user navigation.

**Implemented:** known Home Activity allowlist, no redirection of Amazon Settings windows, bounded curtain lifetime, cancellation of pending Home/app requests, cleanup on service interruption/destruction and explicit user access to Amazon Home. These reduce incorrect redirection but do not eliminate the flash.

**Useful investigations:** measure Home-key-to-visible-frame latency with screen recordings and timestamps; distinguish physical remote behavior from ADB input; map Fire OS routing policy and relevant Activities; evaluate documented platform APIs. Do not infer success from a preferred-Home API's return value alone.

**Acceptance criteria:** the physical Home button reaches TV Light reliably after reboot; launch requests cannot steal focus from an app the user just chose; system Settings, Amazon Home and a recovery path remain accessible; results include transition recordings and tested firmware. No root, disabling verification, permissive SELinux, OTA blocking or speculative removal of Amazon system components as a shortcut.

## 2. Safely stop idle Prime Video when genuinely useful

**Current behavior:** normal background-kill requests do not guarantee Prime exits. The app has no validated automatic force-stop. Optional non-root cleanup is off by default. A foreground check is not a playback check.

**Why this is unresolved:** absence of a MediaSession, a null PlaybackState or no audio player attributed to the primary UID is not proof that playback is stopped. Delegated/native playback, silent video, pause, buffering and stale state can evade simplistic checks. Separate queries have a race window. Amazon services may immediately recreate a process. Killing an app may lose playback state and increase cold-start cost.

**Useful investigations:** correlate media sessions, audio-player ownership, UID/package identity, process state and known on-screen playback conditions. Test real playback, pause, buffering, video without audio, transition to Home and app recreation. Use read-only collection first and publish sanitized fixtures. Simulated parser tests are necessary but do not validate the meaning/completeness of the real device signals.

**Acceptance criteria:** uncertain, inconsistent or incomplete state blocks action; no paused/playing/buffering session is interrupted; any operation is optional and bounded, restricted to an explicitly allowed package/user, and independently verifies its result. No retry storm. Measure memory, latency and recovery rather than claiming benefit from command success. ADB is broad shell authorization, not a secure per-app privilege broker. A future backend needs client authentication and function-specific revocable authorization, with Enforcing and OTA preserved.

## Reports and proposals

Use the issue templates. Include model/codename, shared firmware/kernel version, reproduction steps, expected/actual behavior, collection method and sanitized evidence. Do not include serial numbers, IP/MAC addresses, SSIDs, account identifiers, media titles, raw private dumps, signing/ADB keys or credentials. Do not submit experimental exploits as end-user setup instructions. Contact the owner privately for sensitive findings.
