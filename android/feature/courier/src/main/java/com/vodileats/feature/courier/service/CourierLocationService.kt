package com.vodileats.feature.courier.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vodileats.core.common.constants.LocationConstants
import com.vodileats.core.common.constants.NotificationConstants
import com.vodileats.core.network.api.CourierApi
import com.vodileats.core.network.websocket.TrackingWebSocketClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * KURYER GPS TRACKING FOREGROUND SERVICE
 *
 * Bu servis kuryer ilovasi fonda ishlaganda ham GPS lokatsiyani yig'adi
 * va har 5 soniyada WebSocket va REST API orqali serverga jo'natadi.
 *
 * Asosiy xususiyatlar:
 * - Foreground Service sifatida ishlaydi (Android 8+ uchun majburiy)
 * - FusedLocationProviderClient orqali aniq GPS olish
 * - Har 5 soniyada lokatsiyani WebSocket va REST API orqali yuborish
 * - Battery optimization'dan himoyalangan
 * - Notification orqali foydalanuvchiga ko'rinadi
 */
@AndroidEntryPoint
class CourierLocationService : Service() {

    companion object {
        private const val TAG = "CourierLocationService"

        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"

        fun startService(context: Context) {
            val intent = Intent(context, CourierLocationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CourierLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var courierApi: CourierApi

    @Inject
    lateinit var webSocketClient: TrackingWebSocketClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastLocation: Location? = null
    private var lastBearing: Float = 0f
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🚀 CourierLocationService yaratildi")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * GPS tracking'ni boshlash
     */
    private fun startTracking() {
        if (isTracking) {
            Log.w(TAG, "⚠️ Tracking allaqachon ishlamoqda")
            return
        }

        Log.i(TAG, "📍 GPS Tracking boshlandi")
        isTracking = true

        // Foreground notification ko'rsatish
        val notification = createNotification("Joylashuv kuzatilmoqda...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationConstants.FOREGROUND_SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(
                NotificationConstants.FOREGROUND_SERVICE_NOTIFICATION_ID,
                notification
            )
        }

        // Lokatsiya so'rovlarni boshlash
        requestLocationUpdates()
    }

    /**
     * GPS tracking'ni to'xtatish
     */
    private fun stopTracking() {
        Log.i(TAG, "⏹️ GPS Tracking to'xtatildi")
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * FusedLocationProvider orqali yuqori aniqlikdagi lokatsiya so'rovlarni sozlash
     */
    private fun requestLocationUpdates() {
        // Ruxsatni tekshirish
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ Lokatsiya ruxsati yo'q!")
            stopTracking()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LocationConstants.LOCATION_UPDATE_INTERVAL // 5000ms = 5 soniya
        ).apply {
            setMinUpdateIntervalMillis(LocationConstants.FASTEST_LOCATION_INTERVAL) // 3000ms
            setMinUpdateDistanceMeters(LocationConstants.LOCATION_DISPLACEMENT) // 10 metr
            setWaitForAccurateLocation(false)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.i(TAG, "✅ Lokatsiya so'rovlari ro'yxatdan o'tkazildi (har ${LocationConstants.LOCATION_UPDATE_INTERVAL / 1000} soniya)")
    }

    /**
     * Lokatsiya callback'ni sozlash
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onNewLocation(location)
                }
            }
        }
    }

    /**
     * Yangi lokatsiya kelganda - serverga yuborish
     */
    private fun onNewLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val bearing = if (location.hasBearing()) location.bearing else calculateBearing(location)
        val speed = location.speed

        lastBearing = bearing
        lastLocation = location

        Log.d(TAG, "📍 Yangi lokatsiya: lat=$latitude, lng=$longitude, bearing=$bearing, speed=${speed}m/s")

        // 1. WebSocket orqali tezkor yuborish (real-time)
        webSocketClient.sendCourierLocation(latitude, longitude, bearing)

        // 2. REST API orqali zaxira yuborish (PostgreSQL'ga saqlash uchun)
        serviceScope.launch {
            try {
                val response = courierApi.updateLocation(
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude
                    )
                )
                if (!response.isSuccessful) {
                    Log.w(TAG, "⚠️ REST lokatsiya yangilash muvaffaqiyatsiz: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ REST lokatsiya yuborishda xatolik: ${e.message}")
            }
        }

        // Notification'ni yangilash
        updateNotification(latitude, longitude, speed)
    }

    /**
     * Oldingi va joriy lokatsiya orasidagi bearing'ni hisoblash
     */
    private fun calculateBearing(newLocation: Location): Float {
        val prevLocation = lastLocation ?: return 0f
        return prevLocation.bearingTo(newLocation)
    }

    /**
     * Notification kanalini yaratish
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationConstants.COURIER_TRACKING_CHANNEL_ID,
            NotificationConstants.COURIER_TRACKING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Kuryer joylashuvini kuzatish"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Foreground notification yaratish
     */
    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NotificationConstants.COURIER_TRACKING_CHANNEL_ID)
            .setContentTitle("VODIL EATS - Kuryer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Notification'ni yangi lokatsiya bilan yangilash
     */
    private fun updateNotification(latitude: Double, longitude: Double, speed: Float) {
        val speedKmh = (speed * 3.6).toInt()
        val notification = createNotification(
            "📍 Joylashuv yangilandi | Tezlik: ${speedKmh} km/s"
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            NotificationConstants.FOREGROUND_SERVICE_NOTIFICATION_ID,
            notification
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "🛑 CourierLocationService yo'q qilindi")
        isTracking = false
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
