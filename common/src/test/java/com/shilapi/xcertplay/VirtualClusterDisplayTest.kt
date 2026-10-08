package com.shilapi.xcertplay

import android.content.pm.PackageInfo
import com.shilapi.xcertplay.airplay.AirPlayDisplayConfig
import java.util.concurrent.ExecutorService
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class VirtualClusterDisplayTest {
    private lateinit var activity: CarPlayHostActivity

    @Before fun setUp() {
        // The dashboard map only exists on a BYD.
        shadowOf(RuntimeEnvironment.getApplication().packageManager).installPackage(PackageInfo().apply { packageName = "com.byd.carsettings" })
        activity = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        AirPlayPersistence.saveClusterMapEnabled(activity, false)
    }

    @After fun tearDown() {
        MapMirrors.streamAspect = MapMirrors.PHYSICAL_STREAM_ASPECT
        for (name in listOf("teardownExecutor", "airPlayCommandExecutor")) {
            val executor = activity.javaClass.getDeclaredField(name).apply { isAccessible = true }
                .get(activity) as ExecutorService
            executor.shutdownNow()
        }
    }

    @Test fun virtualStreamRemainsOptIn() { assertNull(config()) }

    @Test fun offABydASwitchLeftOnRequestsNoStream() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager).removePackage("com.byd.carsettings")
        AirPlayPersistence.saveClusterMapEnabled(activity, true)
        assertNull(config())
    }

    @Test fun absentPhysicalClusterNegotiates16By9AndSetsMirrorAspect() {
        AirPlayPersistence.saveClusterMapEnabled(activity, true)
        MapMirrors.streamAspect = MapMirrors.PHYSICAL_STREAM_ASPECT
        val display = config()!!
        assertEquals(1280, display.widthPixels)
        assertEquals(720, display.heightPixels)
        assertEquals(16.0 / 9, MapMirrors.streamAspect, 0.00001)
        assertEquals(display.safeArea!!.left, display.safeArea!!.right)
    }

    private fun config(): AirPlayDisplayConfig? = activity.javaClass
        .getDeclaredMethod("clusterDisplayConfig").apply { isAccessible = true }.invoke(activity) as AirPlayDisplayConfig?
}
