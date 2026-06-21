package mniroy.instaremote

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private var isRecording by mutableStateOf(false)
    private var watchBattery by mutableStateOf(100f)
    private var gpsLevel by mutableStateOf("-")

    private lateinit var locationManager: LocationManager

    // GNSS callback registered at app launch so satellite count shows before recording
    @SuppressLint("MissingPermission")
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var count = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) count++
            }
            gpsLevel = count.toString()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                watchBattery = level * 100f / scale
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkPermissions()
        
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        // Register GNSS callback immediately so satellite count is visible before recording
        startGnssMonitoring()
        // Warm up the GPS location client immediately so lastLocation is populated before recording
        val warmUpIntent = Intent(this, InstaGpsService::class.java).apply {
            action = "WARM_UP"
        }
        startService(warmUpIntent)

        setContent {
            val connectionStatus by viewModel.connectionStatus.observeAsState(false)
            val currentMode by viewModel.currentMode.observeAsState(CaptureMode.RECORD_NORMAL)
            val bleDevices by viewModel.bleDevices.observeAsState(emptyList())

            if (!connectionStatus) {
                // Connection UI
                WearAppConnect(
                    viewModel = viewModel,
                    devices = bleDevices
                )
            } else {
                // Main Camera UI
                WearAppCamera(
                    battery = watchBattery,
                    gpsLevel = gpsLevel,
                    mode = currentMode,
                    isRecording = isRecording,
                    onModeClick = {
                        val nextMode = if (currentMode == CaptureMode.RECORD_NORMAL) CaptureMode.CAPTURE_NORMAL else CaptureMode.RECORD_NORMAL
                        viewModel.setCaptureMode(nextMode)
                    },
                    onRecordClick = { toggleRecording() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // Fires when user presses Home button — disconnect and clean up immediately
        viewModel.bleManager.disconnect()

        // Stop the GPS service
        stopService(Intent(this, InstaGpsService::class.java))

        unregisterReceiver(batteryReceiver)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        }

        // Finish the activity so onDestroy fires and the process is killed
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hard kill — ensures nothing lingers in background
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @SuppressLint("MissingPermission")
    private fun startGnssMonitoring() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Retry after a short delay in case permission was just granted
            Handler(Looper.getMainLooper()).postDelayed({ startGnssMonitoring() }, 2000)
            return
        }
        // Wake GPS chip
        val bundle = android.os.Bundle()
        locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_time_injection", bundle)
        locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "force_xtra_injection", bundle)
        locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(Looper.getMainLooper()))
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    private fun toggleRecording() {
        val connected = viewModel.connectionStatus.value ?: false
        if (!connected) {
            Toast.makeText(this, "Connect camera first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        viewModel.bleManager.startRecording()
        isRecording = true
        
        val intent = Intent(this, InstaGpsService::class.java).apply {
            action = "START_RECORDING"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopRecording() {
        isRecording = false

        // 1. Kill GPS emission immediately so no new GPS chunks can bury the stop command
        startService(Intent(this, InstaGpsService::class.java).apply {
            action = "STOP_RECORDING"
        })

        // 2. Send stop command (with built-in retries in BleManager)
        viewModel.bleManager.stopRecording()
    }
}

@Composable
fun WearAppConnect(
    viewModel: CameraViewModel,
    devices: List<android.bluetooth.BluetoothDevice>
) {
    val isScanning by viewModel.isScanning.observeAsState(false)
    val listState = androidx.wear.compose.foundation.lazy.rememberScalingLazyListState()
    
    MaterialTheme {
        androidx.wear.compose.foundation.lazy.ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "InstaRemote",
                    color = Color.White,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                Button(
                    onClick = { viewModel.startBleScan() },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp),
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFFC0FF00))
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan BLE", color = Color.Black)
                }
            }
            items(devices) { device ->
                @SuppressLint("MissingPermission")
                val name = device.name ?: device.address
                Button(
                    onClick = { viewModel.connectBle(device) },
                    modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(name)
                }
            }
        }
    }
}

@Composable
fun WearAppCamera(
    battery: Float,
    gpsLevel: String,
    mode: CaptureMode,
    isRecording: Boolean,
    onModeClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    val lightGreen = Color(0xFFC0FF00)
    val darkGray = Color(0xFF333333)
    val modeText = if (mode == CaptureMode.RECORD_NORMAL) "VIDEO" else "PHOTO"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Battery Circular Progress
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 8.dp.toPx()
            val startAngle = -90f
            val sweepAngle = (battery / 100f) * 360f

            // Background circle (Dark gray)
            drawArc(
                color = darkGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Battery level circle (Light Green)
            drawArc(
                color = lightGreen,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            // Mode Text
            Text(
                text = modeText,
                color = lightGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onModeClick() }
            )

            // Shutter Button / Timer
            if (isRecording) {
                var ticks by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    while(true) {
                        kotlinx.coroutines.delay(1000)
                        ticks++
                    }
                }
                val hrs = ticks / 3600
                val mins = (ticks % 3600) / 60
                val secs = ticks % 60
                Text(
                    text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                    color = Color.Red,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .clickable { onRecordClick() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { onRecordClick() },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner button styling
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Status Text (GPS only)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val gpsColor = if (gpsLevel == "-" || gpsLevel == "0") Color.Gray else lightGreen
                Text(text = "GPS: $gpsLevel", color = gpsColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
