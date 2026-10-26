# Privacy and repository preparation

The public-facing history is sanitized to remove private LAN addresses and personal author/committer email and workstation names. Historical Python bytecode is removed because generated binaries may preserve local paths. Shared model/build/kernel properties are retained for reproducibility; a firmware fingerprint identifies a software build, not a unique physical device.

No device serial number, Wi-Fi SSID/password, MAC address, account token, private signing key or ADB private key is required to build or use the app. Avoid full dumpsys/logcat captures in public reports. Keep build credentials, local.properties, keys, logs, screenshots and raw diagnostics outside Git. Ignored files are not a security boundary.

History rewriting changes commit IDs. Existing clones must not merge the old history back: preserve any personal work privately and make a fresh clone. Any previously downloaded copy, external fork, cached page or hosting-provider internal object may retain old content. Rewriting the branch does not recall those copies. Ask the hosting provider to remove cached sensitive data if necessary; rotate actual leaked credentials if any are discovered.

This was a targeted review of reachable Git objects and metadata, not a forensic certification or guarantee that every secret format was detected. A private local recovery bundle was kept outside this repository and must never be uploaded.
