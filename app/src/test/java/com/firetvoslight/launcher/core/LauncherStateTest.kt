package com.firetvoslight.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherStateTest {

    private val apps: List<AppEntry> by lazy {
        File("../testing/fixtures/apps_sample.tsv").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split('\t').let { p -> AppEntry(p[0], p[1], p[2]) } }
    }

    private fun key(pkg: String) = apps.first { it.packageName == pkg }.key
    private fun labels(l: List<AppEntry>) = l.map { it.label }

    @Test fun emptyStateSortsAlphabeticallyIgnoringCaseAndAccents() {
        val a = LauncherState().arrange(apps)
        assertTrue(a.favorites.isEmpty())
        assertEquals(apps.size, a.apps.size)
        val l = labels(a.apps)
        assertEquals("Álbum", l.first())
        assertEquals("zeta", l.last())
        assertTrue(l.indexOf("beta") < l.indexOf("Configurações"))
    }

    @Test fun emptyInstalledListGivesEmptySections() {
        val a = LauncherState(listOf("x/y"), listOf("x/y"), setOf("x/y")).arrange(emptyList())
        assertTrue(a.favorites.isEmpty() && a.apps.isEmpty())
        assertEquals(0, a.hiddenCount)
    }

    @Test fun favoritesKeepUserOrderAndLeaveGrid() {
        val s = LauncherState()
        s.toggleFavorite(key("org.videolan.vlc"))
        s.toggleFavorite(key("com.netflix.ninja"))
        val a = s.arrange(apps)
        assertEquals(listOf("VLC", "Netflix"), labels(a.favorites))
        assertFalse("VLC" in labels(a.apps))
        assertEquals(apps.size - 2, a.apps.size)
    }

    @Test fun toggleTwiceRemovesFavorite() {
        val s = LauncherState()
        assertTrue(s.toggleFavorite("a/b"))
        assertFalse(s.toggleFavorite("a/b"))
        assertTrue(s.favorites.isEmpty())
    }

    @Test fun uninstalledFavoriteIsIgnoredNotCrashing() {
        val s = LauncherState(favorites = listOf("gone.pkg/Main", key("com.netflix.ninja")))
        assertEquals(listOf("Netflix"), labels(s.arrange(apps).favorites))
        assertEquals(2, s.favorites.size) // mantido caso o app volte (ex.: atualização)
    }

    @Test fun hideRemovesFromBothSectionsAndCounts() {
        val s = LauncherState()
        val k = key("com.netflix.ninja")
        s.toggleFavorite(k)
        s.hide(k)
        val a = s.arrange(apps)
        assertFalse("Netflix" in labels(a.favorites) + labels(a.apps))
        assertEquals(1, a.hiddenCount)
        assertTrue(s.unhideAll())
        assertTrue("Netflix" in labels(s.arrange(apps).apps))
        assertFalse(s.unhideAll())
    }

    @Test fun favoritingHiddenAppUnhidesIt() {
        val s = LauncherState(hidden = setOf("a/b"))
        s.toggleFavorite("a/b")
        assertFalse("a/b" in s.hidden)
    }

    @Test fun moveInGridPersistsAndNewAppsGoLast() {
        val s = LauncherState()
        val visible = s.arrange(apps).apps.map { it.key }
        val vlc = key("org.videolan.vlc")
        assertEquals(0, s.move(Section.APPS, visible, vlc, -100))
        val after = s.arrange(apps).apps
        assertEquals("VLC", after.first().label)
        val newApp = AppEntry("com.example.aaa", "Main", "AAA")
        assertEquals("AAA", s.arrange(apps + newApp).apps.last().label)
    }

    @Test fun moveFavoritesRightClampsAtEnd() {
        val s = LauncherState()
        listOf("com.netflix.ninja", "org.videolan.vlc", "com.globo.globotv").forEach { s.toggleFavorite(key(it)) }
        val vis = s.arrange(apps).favorites.map { it.key }
        assertEquals(1, s.move(Section.FAVORITES, vis, key("com.netflix.ninja"), 1))
        assertEquals(listOf("VLC", "Netflix", "Globoplay"), labels(s.arrange(apps).favorites))
        val vis2 = s.arrange(apps).favorites.map { it.key }
        assertEquals(2, s.move(Section.FAVORITES, vis2, key("com.netflix.ninja"), 99))
        assertEquals(-1, s.move(Section.FAVORITES, vis2, "missing/x", 1))
    }

    @Test fun moveKeepsUninstalledFavoritesAtTail() {
        val s = LauncherState(favorites = listOf("gone/x", key("com.netflix.ninja"), key("org.videolan.vlc")))
        val vis = s.arrange(apps).favorites.map { it.key }
        s.move(Section.FAVORITES, vis, key("org.videolan.vlc"), -1)
        assertEquals(listOf(key("org.videolan.vlc"), key("com.netflix.ninja"), "gone/x"), s.favorites)
    }

    @Test fun forgetPackageRemovesEverywhere() {
        val k = key("com.netflix.ninja")
        val s = LauncherState(listOf(k), listOf(k), setOf(k))
        assertTrue(s.forgetPackage("com.netflix.ninja"))
        assertTrue(s.favorites.isEmpty() && s.order.isEmpty() && s.hidden.isEmpty())
        assertFalse(s.forgetPackage("com.netflix.ninja"))
        // não remove pacote com prefixo parecido
        val s2 = LauncherState(listOf("com.netflix.ninja2/Main"))
        assertFalse(s2.forgetPackage("com.netflix.ninja"))
    }

    @Test fun duplicateInstalledKeysAreDeduplicated() {
        assertEquals(apps.size, LauncherState().arrange(apps + apps.first()).apps.size)
    }

    @Test fun resetOrderReturnsToAlphabetical() {
        val s = LauncherState()
        val vis = s.arrange(apps).apps.map { it.key }
        s.move(Section.APPS, vis, key("org.videolan.vlc"), -100)
        s.resetOrder()
        assertEquals("Álbum", s.arrange(apps).apps.first().label)
    }
}

class PrefsCodecTest {
    @Test fun roundTrip() {
        val l = listOf("a.b/C", "d.e/F")
        assertEquals(l, PrefsCodec.decode(PrefsCodec.encode(l)))
    }

    @Test fun decodeToleratesNullBlankAndDuplicates() {
        assertTrue(PrefsCodec.decode(null).isEmpty())
        assertTrue(PrefsCodec.decode("").isEmpty())
        assertEquals(listOf("a/b"), PrefsCodec.decode("\n a/b \n\na/b\n"))
    }

    @Test fun appCacheRoundTripAndSanitizes() {
        val l = listOf(AppEntry("a.b", "a.b.Main", "Nome\tcom tab"), AppEntry("c", "c.M", "Ação"))
        val back = PrefsCodec.decodeApps(PrefsCodec.encodeApps(l))
        assertEquals(listOf("Nome com tab", "Ação"), back.map { it.label })
        assertEquals(l.map { it.key }, back.map { it.key })
        assertTrue(PrefsCodec.decodeApps(null).isEmpty())
        assertTrue(PrefsCodec.decodeApps("lixo\nsem\ttabs").isEmpty())
    }

    @Test fun persistedStateRestoresSameArrangement() {
        val s = LauncherState()
        s.toggleFavorite("x/1"); s.toggleFavorite("y/2"); s.hide("z/3")
        val restored = LauncherState(
            PrefsCodec.decode(PrefsCodec.encode(s.favorites)),
            PrefsCodec.decode(PrefsCodec.encode(s.order)),
            PrefsCodec.decode(PrefsCodec.encode(s.hidden)).toSet(),
        )
        assertEquals(s.favorites, restored.favorites)
        assertEquals(s.hidden, restored.hidden)
    }
}

class DefaultHiddenTest {
    @Test fun hidesAmazonClutterKeepsStreamingAndStore() {
        assertTrue("com.amazon.tv.launcher" in DefaultHidden.PACKAGES)
        assertTrue("com.amazon.gamehub" in DefaultHidden.PACKAGES)
        assertFalse("com.amazon.venezia" in DefaultHidden.PACKAGES) // Appstore fica
        assertFalse("com.amazon.firebat" in DefaultHidden.PACKAGES) // Prime fica
        assertFalse("com.netflix.ninja" in DefaultHidden.PACKAGES)
    }

    @Test fun appstoreLaunchComponentIsFixed() {
        assertEquals("com.amazon.venezia.grid.AppsGridLauncherActivity", LaunchFix.BY_PACKAGE["com.amazon.venezia"])
    }
}

class KeyMapTest {
    @Test fun mapsRemoteKeys() {
        assertEquals(KeyAction.UP, KeyMap.map(19))
        assertEquals(KeyAction.RIGHT, KeyMap.map(22))
        assertEquals(KeyAction.SELECT, KeyMap.map(23))
        assertEquals(KeyAction.SELECT, KeyMap.map(66))
        assertEquals(KeyAction.OPTIONS, KeyMap.map(82))
        assertEquals(KeyAction.BACK, KeyMap.map(4))
        assertEquals(KeyAction.NONE, KeyMap.map(85)) // play/pause
    }

    @Test fun moveDeltaRespectsSection() {
        assertEquals(-5, KeyMap.moveDelta(KeyAction.UP, Section.APPS, 5))
        assertEquals(5, KeyMap.moveDelta(KeyAction.DOWN, Section.APPS, 5))
        assertEquals(0, KeyMap.moveDelta(KeyAction.UP, Section.FAVORITES, 5))
        assertEquals(1, KeyMap.moveDelta(KeyAction.RIGHT, Section.FAVORITES, 5))
        assertEquals(0, KeyMap.moveDelta(KeyAction.SELECT, Section.APPS, 5))
    }
}

class DefaultFavoritesTest {
    @Test fun picksOnlyInstalledKnownAppsWithoutPrimeDuplicate() {
        val apps = listOf(
            AppEntry("com.amazon.avod", "A", "Prime old"),
            AppEntry("com.amazon.firebat", "B", "Prime"),
            AppEntry("com.netflix.ninja", "C", "Netflix"),
            AppEntry("org.other", "D", "Other"),
        )
        assertEquals(listOf("Netflix", "Prime"), DefaultFavorites.pick(apps).map { it.label })
    }

    @Test fun respectsMaxAndEmpty() {
        assertTrue(DefaultFavorites.pick(emptyList()).isEmpty())
        val many = DefaultFavorites.KNOWN.map { AppEntry(it, "M", it) }
        assertTrue(DefaultFavorites.pick(many, max = 3).size <= 3)
    }
}

class MemoryPolicyTest {
    private val running = setOf("com.amazon.firebat", "com.netflix.ninja", "com.spotify.tv.android", "org.videolan.vlc", "com.android.systemui")
    private val third = setOf("com.netflix.ninja", "com.spotify.tv.android", "org.videolan.vlc")

    @Test fun defaultKillsOnlyRunningHogs() {
        val k = MemoryPolicy.toKill(MemoryPolicy.DEFAULT_HOGS, false, running, null, "com.firetvoslight.launcher", third)
        assertEquals(listOf("com.amazon.firebat", "com.netflix.ninja"), k)
    }

    @Test fun neverKillsSpotifyOrLauncherOrSystem() {
        val k = MemoryPolicy.toKill(MemoryPolicy.DEFAULT_HOGS, true, running, null, "com.firetvoslight.launcher", third + "com.firetvoslight.launcher")
        assertFalse("com.spotify.tv.android" in k)
        assertFalse("com.firetvoslight.launcher" in k)
        assertFalse("com.android.systemui" in k)
    }

    @Test fun aggressiveKillsThirdPartyPlusKnownHogsExceptExceptions() {
        val k = MemoryPolicy.toKill(MemoryPolicy.DEFAULT_HOGS, true, running, null, "com.firetvoslight.launcher", third)
        // firebat (hog do fabricante) e netflix e vlc; Spotify e systemui preservados.
        assertEquals(listOf("com.amazon.firebat", "com.netflix.ninja", "org.videolan.vlc"), k)
    }

    @Test fun foregroundAppIsNeverKilled() {
        val k = MemoryPolicy.toKill(MemoryPolicy.DEFAULT_HOGS, false, running, "com.netflix.ninja", "com.firetvoslight.launcher", third)
        assertEquals(listOf("com.amazon.firebat"), k)
    }

    @Test fun doesNotKillAppsThatAreNotRunning() {
        val k = MemoryPolicy.toKill(MemoryPolicy.DEFAULT_HOGS, false, setOf("com.netflix.ninja"), null, "x", third)
        assertEquals(listOf("com.netflix.ninja"), k)
    }

    @Test fun systemPrefixesRecognized() {
        assertTrue(MemoryPolicy.isSystem("com.amazon.tv.launcher"))
        assertTrue(MemoryPolicy.isSystem("android"))
        assertTrue(MemoryPolicy.isSystem("com.android.systemui"))
        assertFalse(MemoryPolicy.isSystem("com.netflix.ninja"))
    }
}

/** Medição no Mac (JVM). Não equivale ao desempenho no Fire Stick. */
class ArrangeBenchmarkTest {
    @Test fun arrange500AppsUnder50msOnJvm() {
        val apps = (0 until 500).map { AppEntry("pkg.n$it", "Main", "App ${(it * 7919) % 500}") }
        val s = LauncherState(favorites = apps.take(8).map { it.key }, order = apps.shuffled().take(200).map { it.key })
        repeat(20) { s.arrange(apps) } // aquecimento
        val t0 = System.nanoTime()
        repeat(50) { s.arrange(apps) }
        val ms = (System.nanoTime() - t0) / 50 / 1e6
        File("../testing/benchmarks/jvm-arrange.txt").writeText("arrange(500) avg_ms=%.3f (Mac/JVM)\n".format(ms))
        assertTrue("arrange too slow: $ms ms", ms < 50)
    }
}
