package com.shilapi.xcertplay

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import com.shilapi.xcertplay.compat.systemService
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.network.CarHotspotSettings
import com.shilapi.xcertplay.network.CarHotspotTethering

internal enum class CarPowerStep { CAR_STARTED, WAIT_FOR_CAR_OFF, CAR_STOPPED, NOTHING }

/**
 * Sleep and wake with the car, for a tablet that the car charges: charging starting means the car
 * started, and charging stopped for [GRACE_MILLIS] means it was turned off. The grace period rides
 * out the dip while the engine cranks. Android wakes the screen by itself when power is plugged in;
 * turning it off needs the driver to allow [CarPowerAdmin] once.
 */
internal object CarPower {
    const val ACTION_CHECK = "com.shihab.diplay.CAR_POWER_CHECK"
    const val EXTRA_CAR_STARTED = "car_power_started"
    const val GRACE_MILLIS = 5_000L
    private const val TAG = "DiPlay-CarPower"

    private fun prefs(context: Context) = context.getSharedPreferences("diplay", Context.MODE_PRIVATE)
    fun enabled(context: Context): Boolean = prefs(context).getBoolean("follow_car_power", false)
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("follow_car_power", enabled).apply()
        if (!enabled) {
            cancelCheck(context)
            // A device admin blocks uninstalling DiPlay, so give it up with the setting.
            if (canTurnScreenOff(context)) context.systemService<DevicePolicyManager>()?.removeActiveAdmin(screenOffAdmin(context))
        }
    }

    fun screenOffAdmin(context: Context) = ComponentName(context, CarPowerAdmin::class.java)
    fun canTurnScreenOff(context: Context): Boolean =
        context.systemService<DevicePolicyManager>()?.isAdminActive(screenOffAdmin(context)) == true

    /** Android's "Activate device administrator?" screen for [CarPowerAdmin]. */
    fun allowScreenOffIntent(context: Context): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, screenOffAdmin(context))
        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.settings_car_power_screen_off_explanation))

    private fun battery(context: Context): Intent? =
        runCatching { context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()

    /** Head units have no battery and are always powered, so the setting means nothing there. */
    fun hasBattery(context: Context): Boolean =
        battery(context)?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) == true

    fun charging(context: Context): Boolean = (battery(context)?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

    fun step(action: String?, enabled: Boolean, charging: Boolean): CarPowerStep = when {
        !enabled -> CarPowerStep.NOTHING
        action == Intent.ACTION_POWER_CONNECTED -> CarPowerStep.CAR_STARTED
        action == Intent.ACTION_POWER_DISCONNECTED -> CarPowerStep.WAIT_FOR_CAR_OFF
        action == ACTION_CHECK && !charging -> CarPowerStep.CAR_STOPPED
        else -> CarPowerStep.NOTHING
    }

    private fun checkIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, CarPowerReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun scheduleCheck(context: Context) {
        context.systemService<AlarmManager>()?.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + GRACE_MILLIS, checkIntent(context))
    }

    fun cancelCheck(context: Context) {
        context.systemService<AlarmManager>()?.cancel(checkIntent(context))
    }

    fun carStarted(context: Context) {
        cancelCheck(context)
        if (CarPlayBackgroundSession.hasSession()) return
        Log.i(TAG, "charging started; opening DiPlay to connect")
        context.startActivity(Intent(context, DiPlayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_CAR_STARTED, true))
    }

    fun carStopped(context: Context) {
        Log.i(TAG, "charging stopped for ${GRACE_MILLIS / 1000} s; disconnecting so the tablet can sleep")
        // The hotspot stays up until the session ends, so the iPhone still hears the disconnect.
        CarPlayBackgroundSession.stop {
            if (CarHotspotSettings.enabled(context)) CarHotspotTethering.disableSoftAp(context)
            turnScreenOff(context)
        }
    }

    private fun turnScreenOff(context: Context) {
        if (!canTurnScreenOff(context)) return
        Log.i(TAG, "turning the screen off")
        runCatching { context.systemService<DevicePolicyManager>()?.lockNow() }
            .onFailure { Log.w(TAG, "could not turn the screen off", it) }
    }
}

/** Lets [CarPower] turn the screen off when the car is turned off. */
class CarPowerAdmin : DeviceAdminReceiver()

class CarPowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (CarPower.step(intent.action, CarPower.enabled(app), CarPower.charging(app))) {
            CarPowerStep.CAR_STARTED -> CarPower.carStarted(app)
            CarPowerStep.WAIT_FOR_CAR_OFF -> CarPower.scheduleCheck(app)
            CarPowerStep.CAR_STOPPED -> CarPower.carStopped(app)
            CarPowerStep.NOTHING -> Unit
        }
    }
}
