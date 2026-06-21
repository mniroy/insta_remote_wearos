package mniroy.instaremote

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class NativeBleManager private constructor(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = adapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: NativeBleManager? = null

        fun getInstance(context: Context): NativeBleManager {
            return instance ?: synchronized(this) {
                instance ?: NativeBleManager(context.applicationContext).also { instance = it }
            }
        }

        val LBS_SERVICE  = UUID.fromString("0000BE80-0000-1000-8000-00805F9B34FB")
        val LBS_RW_CHAR  = UUID.fromString("0000BE81-0000-1000-8000-00805F9B34FB")
        val LBS_NOTIF_CHAR = UUID.fromString("0000BE82-0000-1000-8000-00805F9B34FB")

        val CMD_START_REC = byteArrayOf(0x12, 0, 0, 0, 0x4, 0, 0, 0x4, 0, 0x2, 0xff.toByte(), 0, 0, 0x80.toByte(), 0, 0, 0x8, 0x1)
        val CMD_STOP_REC  = byteArrayOf(0x12, 0, 0, 0, 0x4, 0, 0, 0x5, 0, 0x2, 0xff.toByte(), 0, 0, 0x80.toByte(), 0, 0, 0x10, 0x1)
        val CMD_STANDBY   = byteArrayOf(0x7, 0, 0, 0, 0x5, 0, 0)
    }

    private var seqNo = 1

    // Two separate queues: priority for control commands, low-priority for GPS telemetry
    private val controlQueue = ArrayDeque<ByteArray>()
    private val gpsQueue     = ArrayDeque<ByteArray>()

    // Gate: set to false when stop is requested to prevent GPS chunks from flooding the queue
    private var gpsEnabled   = false

    private var isWriting    = false
    private var lastWriteTime = 0L
    private val mainHandler  = Handler(Looper.getMainLooper())

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val current = _scannedDevices.value.toMutableList()
            if (current.none { it.address == device.address }) {
                current.add(device)
                _scannedDevices.value = current
            }
        }
    }

    fun startScan() {
        if (scanner == null) return
        _scannedDevices.value = emptyList()
        val filter   = ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(LBS_SERVICE)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        mainHandler.postDelayed({ stopScan() }, 10_000)
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = true
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = false
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    writeChar = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service   = gatt.getService(LBS_SERVICE)
                    writeChar     = service?.getCharacteristic(LBS_RW_CHAR)
                    val notifChar = service?.getCharacteristic(LBS_NOTIF_CHAR)
                    if (notifChar != null) {
                        gatt.setCharacteristicNotification(notifChar, true)
                        val desc = notifChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (desc != null) {
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(desc)
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                isWriting = false
                processQueue()
            }
        })
    }

    fun disconnect() {
        controlQueue.clear()
        gpsQueue.clear()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeChar     = null
        _connectionState.value = false
    }

    // ── Control commands (START / STOP) ──────────────────────────────────────
    fun startRecording() {
        gpsEnabled = true
        sendControlCommand(CMD_START_REC)
    }

    fun stopRecording() {
        // Gate GPS immediately — no new GPS chunks after this point
        gpsEnabled = false
        // Send stop command and retry up to 3 times to guarantee delivery
        sendControlCommand(CMD_STOP_REC)
        mainHandler.postDelayed({ sendControlCommand(CMD_STOP_REC) }, 400)
        mainHandler.postDelayed({ sendControlCommand(CMD_STOP_REC) }, 900)
    }

    /**
     * High-priority path.
     * Clears any queued GPS telemetry so the command goes out immediately.
     */
    private fun sendControlCommand(payload: ByteArray) {
        val message = payload.copyOf()
        if (message.size > 10) {
            seqNo = if (seqNo >= 254) 1 else seqNo + 1
            message[10] = seqNo.toByte()
        }

        // Drop all queued GPS data — control commands take priority
        gpsQueue.clear()
        isWriting = false   // Force-unlock in case GPS write was in-flight

        if (message.size > 20) {
            message.toList().chunked(20).forEach { controlQueue.addLast(it.toByteArray()) }
        } else {
            controlQueue.addLast(message)
        }
        controlQueue.addLast(CMD_STANDBY)

        processQueue()
    }

    // ── GPS telemetry (low-priority) ──────────────────────────────────────────
    fun sendGpsData(lat: Double, lon: Double, speed: Float, heading: Float, altitude: Double, timeMs: Long) {
        // Gate: don't accept GPS data if stop has been requested
        if (!gpsEnabled) return
        // If control commands are queued, skip this GPS frame entirely — don't back up
        if (controlQueue.isNotEmpty()) return
        // Only keep the latest GPS frame — drop stale ones
        gpsQueue.clear()

        val buffer = ByteBuffer.allocate(71).order(ByteOrder.LITTLE_ENDIAN)
        val header = byteArrayOf(
            0x47, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x35, 0x00,
            0x02, 0xff.toByte(), 0x00, 0x00, 0x80.toByte(), 0x00, 0x00, 0x0a, 0x35
        )
        buffer.put(header)
        buffer.putInt((timeMs / 1000).toInt())
        buffer.put(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x41))
        buffer.putDouble(Math.abs(lat))
        buffer.put((if (lat >= 0) 0x4e else 0x53).toByte())
        buffer.putDouble(Math.abs(lon))
        buffer.put((if (lon >= 0) 0x45 else 0x57).toByte())
        buffer.putDouble(Math.abs(speed.toDouble()))
        var headDeg = heading.toDouble(); if (headDeg < 0) headDeg += 360.0
        buffer.putDouble(headDeg)
        buffer.putDouble(Math.abs(altitude))

        buffer.array().toList().chunked(20).forEach { gpsQueue.addLast(it.toByteArray()) }
        processQueue()
    }

    // ── Queue processor ───────────────────────────────────────────────────────
    private fun processQueue() {
        if (writeChar == null || bluetoothGatt == null) return

        // Safety timeout: if a write hasn't completed in 500ms, force-unlock
        if (isWriting && System.currentTimeMillis() - lastWriteTime > 500) {
            Log.w("BLE", "Write timeout — force-unlocking queue")
            isWriting = false
        }
        if (isWriting) return

        // Control queue has absolute priority
        val chunk = when {
            controlQueue.isNotEmpty() -> controlQueue.removeFirst()
            gpsQueue.isNotEmpty()     -> gpsQueue.removeFirst()
            else                      -> return
        }

        isWriting = true
        writeChar?.value = chunk
        val ok = bluetoothGatt?.writeCharacteristic(writeChar) == true
        if (ok) {
            lastWriteTime = System.currentTimeMillis()
        } else {
            // BLE stack busy — put chunk back and retry later
            isWriting = false
            if (controlQueue.isNotEmpty() || chunk.size <= 7) {
                controlQueue.addFirst(chunk)
            } else {
                gpsQueue.addFirst(chunk)
            }
            mainHandler.postDelayed({ processQueue() }, 50)
        }
    }
}
