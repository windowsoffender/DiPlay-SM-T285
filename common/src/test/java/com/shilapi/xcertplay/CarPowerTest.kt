package com.shilapi.xcertplay

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class CarPowerTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val alarms get() = shadowOf(context.getSystemService(AlarmManager::class.java))

    @Before fun setUp() {
        context.getSharedPreferences("diplay", 0).edit().clear().commit()
        CarPlayBackgroundSession.stop()
    }

    @Test fun nothingHappensWhileTheSettingIsOff() {
        for (action in listOf(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED, CarPower.ACTION_CHECK)) {
            assertEquals(CarPowerStep.NOTHING, CarPower.step(action, enabled = false, charging = false))
        }
    }

    @Test fun chargingStartingMeansTheCarStarted() {
        assertEquals(CarPowerStep.CAR_STARTED, CarPower.step(Intent.ACTION_POWER_CONNECTED, enabled = true, charging = true))
    }

    @Test fun chargingStoppingWaitsBeforeDisconnecting() {
        assertEquals(CarPowerStep.WAIT_FOR_CAR_OFF,
            CarPower.step(Intent.ACTION_POWER_DISCONNECTED, enabled = true, charging = false))
    }

    @Test fun theCarIsOffOnlyIfStillNotChargingAfterTheWait() {
        assertEquals(CarPowerStep.CAR_STOPPED, CarPower.step(CarPower.ACTION_CHECK, enabled = true, charging = false))
        assertEquals(CarPowerStep.NOTHING, CarPower.step(CarPower.ACTION_CHECK, enabled = true, charging = true))
    }

    @Test fun unpluggingSchedulesTheCheckAfterTheGracePeriod() {
        CarPower.setEnabled(context, true)
        CarPowerReceiver().onReceive(context, Intent(Intent.ACTION_POWER_DISCONNECTED))

        val alarm = assertNotNull(alarms.nextScheduledAlarm)
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarm.type)
        assertEquals(SystemClock.elapsedRealtime() + CarPower.GRACE_MILLIS, alarm.triggerAtTime)
    }

    @Test fun pluggingBackInDuringTheGracePeriodKeepsTheConnection() {
        CarPower.setEnabled(context, true)
        CarPowerReceiver().onReceive(context, Intent(Intent.ACTION_POWER_DISCONNECTED))
        CarPowerReceiver().onReceive(context, Intent(Intent.ACTION_POWER_CONNECTED))

        assertNull(alarms.nextScheduledAlarm)
    }

    @Test fun pluggingInOpensDiPlayToConnect() {
        CarPower.setEnabled(context, true)
        CarPowerReceiver().onReceive(context, Intent(Intent.ACTION_POWER_CONNECTED))

        val started = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(DiPlayActivity::class.java.name, started.component?.className)
        assertTrue(started.getBooleanExtra(CarPower.EXTRA_CAR_STARTED, false))
    }

    @Test fun turningTheSettingOffCancelsAPendingCheck() {
        CarPower.setEnabled(context, true)
        CarPowerReceiver().onReceive(context, Intent(Intent.ACTION_POWER_DISCONNECTED))
        CarPower.setEnabled(context, false)

        assertNull(alarms.nextScheduledAlarm)
    }

    private fun <T> assertNotNull(value: T?): T {
        org.junit.Assert.assertNotNull(value)
        return value!!
    }
}
