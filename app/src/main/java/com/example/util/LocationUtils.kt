package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /** Dernière position connue (approximative). Retourne null si la permission est absente ou s'il n'y en a pas. */
    @SuppressLint("MissingPermission")
    fun lastKnownLocation(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = manager.getProviders(true)
            var best: android.location.Location? = null
            for (provider in providers) {
                val loc = manager.getLastKnownLocation(provider) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            }
            best?.let { Pair(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    /** Distance en mètres (formule de haversine). */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    fun formatDistance(meters: Int): String =
        if (meters < 1000) "à ${(meters / 50) * 50 + 50} m" else "à ${String.format(java.util.Locale.US, "%.1f", meters / 1000.0)} km"
}
