package mniroy.instaremote

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class CaptureMode {
    RECORD_NORMAL,
    CAPTURE_NORMAL,
    TIMELAPSE
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    val bleManager = NativeBleManager.getInstance(application)

    private val _connectionStatus = MutableLiveData<Boolean>(false)
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    private val _batteryLevel = MutableLiveData<Int>(0)
    val batteryLevel: LiveData<Int> = _batteryLevel

    private val _currentMode = MutableLiveData<CaptureMode>(CaptureMode.RECORD_NORMAL)
    val currentMode: LiveData<CaptureMode> = _currentMode

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _bleDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val bleDevices: LiveData<List<BluetoothDevice>> = _bleDevices

    init {
        viewModelScope.launch {
            bleManager.connectionState.collect {
                _connectionStatus.postValue(it)
            }
        }
        viewModelScope.launch {
            bleManager.scannedDevices.collect {
                _bleDevices.postValue(it)
            }
        }
    }

    fun startBleScan() {
        _isScanning.value = true
        bleManager.startScan()
        // Stop scanning UI after 10s
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isScanning.value = false
        }, 10000)
    }

    fun connectBle(device: BluetoothDevice) {
        bleManager.connect(device)
    }

    fun setCaptureMode(mode: CaptureMode) {
        _currentMode.value = mode
        // For now, custom BLE ignores mode switching in UI and just relies on startRecording
    }
}
