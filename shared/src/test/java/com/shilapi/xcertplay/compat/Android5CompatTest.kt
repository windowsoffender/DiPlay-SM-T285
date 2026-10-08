package com.shilapi.xcertplay.compat

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Robolectric starts at API 23, so these tests report API 22 and check that the Android 5.1 branch
 * is taken. The APK's dex is checked separately for calls that Android 5.1 lacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], manifest = Config.NONE)
class Android5CompatTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val realSdk = Build.VERSION.SDK_INT

    private fun reportSdk(sdk: Int) = ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)

    @After fun restoreSdk() = reportSdk(realSdk)

    @Test fun systemServicesResolveByName() {
        reportSdk(22)

        assertNotNull(context.systemService<AudioManager>())
        assertNotNull(context.systemService<ConnectivityManager>())
        assertNotNull(context.systemService<WifiManager>())
    }

    @Test fun installTimePermissionsCountAsGranted() {
        reportSdk(22)

        assertTrue(canDrawOverlays(context))
        assertTrue(canWriteSystemSettings(context))
    }

    @Test fun aConfigurationHasOneLocale() {
        reportSdk(22)
        val source = Configuration().apply { setLocale(Locale("ar")) }
        val copy = Configuration().apply { setLocalesFrom(source) }

        assertEquals(Locale("ar"), copy.primaryLocale)
        assertEquals(View.LAYOUT_DIRECTION_RTL, copy.layoutDirection)
    }

    @Config(sdk = [24])
    @Test fun api24CopiesTheWholeLocaleList() {
        val source = Configuration().apply { setLocales(LocaleList(Locale("uk"), Locale.ENGLISH)) }
        val copy = Configuration().apply { setLocalesFrom(source) }

        assertEquals(source.locales, copy.locales)
        assertEquals(Locale("uk"), copy.primaryLocale)
    }

    @Test fun anEmptyConfigurationHasNoLocale() {
        assertNull(Configuration().primaryLocale)
    }

    @Test fun onlyAFrameLayoutGetsAForeground() {
        reportSdk(22)
        val ring = ColorDrawable(Color.RED)
        val frame = FrameLayout(context).apply { setForegroundCompat(ring) }
        val row = LinearLayout(context).apply { setForegroundCompat(ring) }

        assertSame(ring, frame.foreground)
        assertNull(row.foreground)
    }

    @Test fun api23GivesAnyViewAForeground() {
        val ring = ColorDrawable(Color.RED)
        val row = LinearLayout(context).apply { setForegroundCompat(ring) }

        assertSame(ring, row.foreground)
    }

    @Test fun switchColorsTintTheDrawables() {
        reportSdk(22)
        val switch = Switch(context)
        switch.setTintListsCompat(ColorStateList.valueOf(Color.RED), ColorStateList.valueOf(Color.BLUE))

        assertNull(switch.thumbTintList)
        assertNull(switch.trackTintList)
    }

    @Test fun aStreamTrackIsBuiltWithTheConstructor() {
        reportSdk(22)
        val track = streamAudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(48_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),
            19_200,
        )
        try {
            assertEquals(AudioTrack.STATE_INITIALIZED, track.state)
            assertEquals(960, track.writeBlocking(ByteArray(960), 0, 960))
            assertEquals(0, track.underrunCountCompat)
            assertEquals(4_800, track.bufferSizeInFramesCompat(4_800))
            assertNull(track.routedDeviceType)
        } finally {
            track.release()
        }
    }
}
