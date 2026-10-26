package com.firetvoslight.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test
import com.firetvoslight.launcher.core.HomeRedirectPolicy.Action

class HomeRedirectPolicyTest {
    private val self = "com.firetvoslight.launcher"
    private fun action(pkg: String?, bypass: Boolean = false, launching: String? = null, window: String? = "com.amazon.tv.launcher.ui.HomeActivity_vNext") =
        HomeRedirectPolicy.decide(pkg, self, bypass, launching, window)

    @Test fun amazonSettingsNeverRedirect() {
        for (window in listOf("com.amazon.tv.launcher.ui.SettingsActivity",
            "com.amazon.tv.launcher.ui.MainSettingsActivity", "android.app.Dialog", null)) {
            assertEquals(Action.LEAVE, action("com.amazon.tv.launcher", window = window))
        }
    }

    @Test fun settingsDismissPendingHomeWhileAppIsLaunching() {
        assertEquals(Action.LEAVE, action("com.amazon.tv.launcher", launching = "com.netflix.ninja",
            window = "com.amazon.tv.launcher.ui.MainSettingsActivity"))
    }

    @Test fun redirectsOnlyAmazonHome() {
        assertEquals(Action.REDIRECT, action("com.amazon.tv.launcher"))
        assertEquals(Action.LEAVE, action("com.amazon.tv.launcher.other"))
    }

    @Test fun respectsExplicitAmazonBypass() {
        assertEquals(Action.IGNORE, action("com.amazon.tv.launcher", bypass = true))
    }

    @Test fun homeCannotStealChosenApplicationLaunch() {
        assertEquals(Action.IGNORE, action("com.amazon.tv.launcher", launching = "com.netflix.ninja"))
    }

    @Test fun chosenApplicationDismissesCurtain() {
        assertEquals(Action.DISMISS, action("com.netflix.ninja", launching = "com.netflix.ninja"))
    }

    @Test fun explicitlyChosenAmazonHomeIsAlsoReleased() {
        assertEquals(Action.DISMISS, action("com.amazon.tv.launcher", launching = "com.amazon.tv.launcher"))
    }

    @Test fun otherApplicationReleasesPendingRedirect() {
        assertEquals(Action.LEAVE, action("com.amazon.firebat"))
        assertEquals(Action.LEAVE, action("com.amazon.tv.settings.v2", bypass = true))
        assertEquals(Action.LEAVE, action("org.other", launching = "com.netflix.ninja"))
    }

    @Test fun ownAndSystemWindowsDoNotCreateRedirects() {
        for (pkg in listOf(self, "android", "com.android.systemui")) {
            assertEquals(Action.IGNORE, action(pkg))
        }
    }

    @Test fun missingPackageDoesNotCreateRedirect() {
        for (pkg in listOf(null, "", "  ")) assertEquals(Action.IGNORE, action(pkg))
    }

    @Test fun bypassDoesNotKeepCurtainAboveChosenApplication() {
        assertEquals(Action.DISMISS, action("com.netflix.ninja", bypass = true, launching = "com.netflix.ninja"))
    }
}
