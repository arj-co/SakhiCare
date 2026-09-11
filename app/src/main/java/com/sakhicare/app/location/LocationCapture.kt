package com.sakhicare.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float?,
    val capturedAt: Long
)

/**
 * Reads the last device location without requiring a network connection.
 * A missing permission/provider is a valid offline outcome and returns null.
 */
class LocationCapture(private val context: Context) {
    fun lastKnown(): CapturedLocation? {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val locations = buildList {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                try {
                    manager.getLastKnownLocation(provider)?.let(::add)
                } catch (_: SecurityException) {
                    // Permission can be revoked between the check and provider read.
                }
            }
        }
        val best: Location = locations.maxByOrNull { it.time } ?: return null
        return CapturedLocation(
            latitude = best.latitude,
            longitude = best.longitude,
            accuracyM = if (best.hasAccuracy()) best.accuracy else null,
            capturedAt = best.time
        )
    }
}
