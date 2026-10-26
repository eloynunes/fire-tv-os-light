# Security policy and current risk assessment

## Scope

This is a limited source review and device validation, not a full independent security audit. No claim is made that TV Light is free of vulnerabilities. Version 0.2.0 does not obtain root, probe su, change SELinux, block OTA or expose a shell server. Its manifest has no INTERNET permission. SELinux remained Enforcing during the device checks.

## Known risks

1. **Accessibility and overlays:** the opt-in service receives window-state events and can draw a full-screen accessibility overlay and launch apps. The manifest protects the service with BIND_ACCESSIBILITY_SERVICE and does not request window-content retrieval. Bugs can still obscure UI or redirect focus incorrectly. Disable the service in system accessibility settings if this occurs.
2. **Timing and lifecycle:** asynchronous events and already-delivered alarms cannot be rolled back atomically. Pending requests are cancelled on observed transitions, timeout and interruption, but this does not prove all possible event orderings safe.
3. **Signing:** the existing deployment used a local debug certificate. This is not production signing or a secure update distribution process. Do not publish private keystores or trust arbitrary APKs with the same name. A future production-key migration needs an explicit compatibility/data plan.
4. **Legacy ADB utilities:** shell-authorized ADB has broad privileges. Foreground-only checks cannot establish absence of playback. Legacy memory/agent tools can interrupt playback, create persistent macOS LaunchAgents and compete with launcher navigation. They are disabled by default and are not part of the recommended app workflow.
5. **Other mutating scripts:** home-lock.sh replaces the enabled accessibility-service list and its off operation disables accessibility globally; this may affect other services. Debloat, drift, speed, sleep and restore scripts modify settings/packages. Restoration is partial and depends on saved state; uninstall is not a full rollback. Inspect each script before use.
6. **Memory cleanup:** optional non-root requests may disrupt background applications, add cold-start cost or provide no benefit. Do not infer playback safety from foreground state or infer RAM savings from a successful API call.
7. **Historical root research:** GhostLock failed in the recorded trial. It is not supported, bundled or recommended. The examined third-party method attempts permissive SELinux, unauthenticated local shells and OTA changes. There is no validated unbrick procedure for this unit. Ignore historical claims that it automatically installs Magisk or guarantees Home replacement.
8. **Diagnostics and history:** logs/screenshots can expose identifiers, app usage and media/account information. Ignoring a file does not remove previously committed copies. Before changing visibility, review the entire Git history; this pass is not a forensic history scan or secret-scanner certification.

## Reporting

Do not post exploit details or sensitive device data in public issues. Contact the repository owner privately through an available GitHub contact route. If GitHub private vulnerability reporting is enabled, prefer it; availability is not assumed here. No response SLA is promised.
