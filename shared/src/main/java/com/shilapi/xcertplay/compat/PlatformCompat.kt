package com.shilapi.xcertplay.compat

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Locale

/** Releases the channel on API 27+. Earlier releases have no close call and free it when collected. */
fun WifiP2pManager.Channel.closeCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) close()
}

/** [LocationManager.isLocationEnabled] needs API 28; before that a location provider must be on. */
fun LocationManager.isLocationEnabledCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isLocationEnabled
    } else {
        isProviderEnabled(LocationManager.GPS_PROVIDER) || isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

/** Looking a system service up by class needs API 23; before that [ContextCompat] maps the class to its name. */
inline fun <reified T : Any> Context.systemService(): T? = ContextCompat.getSystemService(this, T::class.java)

/** Before API 23 every permission in the manifest is granted at install time. */
fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** [Settings.System.canWrite] needs API 23; before that WRITE_SETTINGS is granted at install time. */
fun canWriteSystemSettings(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)

/** [Settings.canDrawOverlays] needs API 23; before that SYSTEM_ALERT_WINDOW is granted at install time. */
fun canDrawOverlays(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

/** [ConnectivityManager.getActiveNetwork] needs API 23; before that the network that is connected with the active type. */
@Suppress("DEPRECATION")
fun ConnectivityManager.activeNetworkCompat(): Network? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return activeNetwork
    val active = activeNetworkInfo?.takeIf { it.isConnected } ?: return null
    return allNetworks.firstOrNull { network ->
        getNetworkInfo(network)?.let { it.type == active.type && it.isConnected } == true
    }
}

/** The first locale, or null if there is none. [Configuration.getLocales] needs API 24; before that there is one. */
@Suppress("DEPRECATION")
val Configuration.primaryLocale: Locale?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locales[0] else locale

/** Copies the locales of [source]. [Configuration.setLocales] needs API 24; before that its one locale. */
@Suppress("DEPRECATION")
fun Configuration.setLocalesFrom(source: Configuration) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) setLocales(source.locales) else setLocale(source.locale)
}
