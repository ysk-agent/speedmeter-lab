package dev.moriya.speedmeterlab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the meter after the events that kill it while the enabled
 * preference stays on: an app update (MY_PACKAGE_REPLACED) and a
 * device reboot (BOOT_COMPLETED). Both broadcasts are exempt from
 * the background foreground-service start restrictions.
 *
 * Without this, every update silently stopped the meter until the
 * user happened to open the app — which, in a lab app updated many
 * times a day, read as "the display stopped working".
 */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return
        try {
            context.startForegroundService(Intent(context, SpeedMeterService::class.java))
        } catch (e: Exception) {
            // If the start is refused, the activity's onResume
            // reconcile is the fallback; nothing else to do here.
        }
    }
}
