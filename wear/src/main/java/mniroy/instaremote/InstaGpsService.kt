package mniroy.instaremote

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

import kotlinx.coroutines.*

class InstaGpsService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationManager: LocationManager

    private var satelliteCount = 0
    private var lastLocation: Location? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())


    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastLocation = location
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "WARM_UP"         -> warmUp()       // App opened — start getting location, don't emit
            "START_RECORDING" -> startTracking()
            "STOP_RECORDING"  -> stopTracking()
        }
        return START_STICKY
    }

    // Start location updates silently so lastLocation is populated before recording
    private fun warmUp() {
        requestLocationUpdates()
    }

    private fun startTracking() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "InstaGpsChannel")
            .setContentTitle("InstaRemote")
            .setContentText("Injecting GPS data...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
            
        startForeground(1, notification)
        requestLocationUpdates()

        // 1Hz GPS emission — BLE cannot reliably handle more than this
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            while (isActive) {
                sendCurrentGpsToCamera()
                delay(1000) // 1Hz
            }
        }
    }

    private fun stopTracking() {
        serviceJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Wake GPS chip and ensure it tracks while recording
        val bundle = android.os.Bundle()
        locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", bundle)
        locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", bundle)

        // 1. Fused Location Client (For Google Play Services magic)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
            .setMinUpdateIntervalMillis(100)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 2. Explicitly request GPS_PROVIDER to forcefully wake the hardware chip on Wear OS
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Let FusedLocationClient handle the actual data, we just want to force the chip on
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                },
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun broadcastGpsStatus(satellites: Int) {
        val intent = Intent("GPS_STATUS_UPDATE")
        intent.putExtra("level", satellites.toString())
        sendBroadcast(intent)
    }

    private fun sendCurrentGpsToCamera() {
        val loc = lastLocation
        val lat = loc?.latitude ?: 0.0
        val lon = loc?.longitude ?: 0.0
        val speed = if (loc?.hasSpeed() == true) loc.speed else 0f
        val heading = if (loc?.hasBearing() == true) loc.bearing else 0f
        val altitude = if (loc?.hasAltitude() == true) loc.altitude else 0.0
        val timeMs = loc?.time ?: System.currentTimeMillis()

        NativeBleManager.getInstance(this).sendGpsData(lat, lon, speed, heading, altitude, timeMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "InstaGpsChannel",
                "Insta360 GPS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
