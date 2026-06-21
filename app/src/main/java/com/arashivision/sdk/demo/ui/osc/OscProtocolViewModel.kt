package com.arashivision.sdk.demo.ui.osc

import androidx.lifecycle.viewModelScope
import com.arashivision.insta360.basecamera.camera.CameraType
import com.arashivision.sdk.demo.base.BaseViewModel
import com.arashivision.sdk.demo.ext.connectivityManager
import com.arashivision.sdk.demo.ext.instaCameraManager
import com.arashivision.sdk.demo.model.CaptureExposureData
import com.arashivision.sdk.demo.osc.OscManager
import com.arashivision.sdk.demo.osc.callback.IOscCallback
import com.arashivision.sdk.demo.osc.delegate.OscRequestDelegate
import com.arashivision.sdk.demo.util.NetworkManager
import com.arashivision.sdk.demo.util.StorageUtils
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.okgo.OkGo
import com.arashivision.sdkcamera.okgo.callback.FileCallback
import com.arashivision.sdkcamera.okgo.model.Response
import com.arashivision.sdkmedia.stitch.StitchUtils
import com.arashivision.sdkmedia.work.WorkWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class OscProtocolViewModel : BaseViewModel() {

    private val oscManager: OscManager = OscManager.getInstance()

    private var captureExposureData: CaptureExposureData? = null
    private var frontSensorCapturePath: String? = null
    private var rearSensorCapturePath: String? = null
    private var stitchOutputPath: String? = null
    private var infoText: String? = null
    private var frontCapturePending = false
    private var rearCapturePending = false

    init {
        oscManager.setOscRequestDelegate(OscRequestDelegate())
    }

    fun initOsc() {
        val separatedFisheyeSupported = CameraType.getForType(instaCameraManager.cameraType) in listOf(
            CameraType.ONEX2,
            CameraType.X3,
            CameraType.X4
        )
        emitEvent(OscProtocolEvent.LayoutFeatureEvent(separatedFisheyeSupported))
        emitResult()
    }

    fun requestInfo() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            oscManager.customRequest("/osc/info", null, object : IOscCallback {
                override fun onStartRequest() = emitLoading(true, "Requesting /osc/info")

                override fun onSuccessful(objectResult: Any?) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    infoText = objectResult?.toString()
                    emitResult()
                }

                override fun onError(message: String) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent(message))
                }
            })
        }
    }

    fun requestState() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            oscManager.customRequest("/osc/state", "", object : IOscCallback {
                override fun onStartRequest() = emitLoading(true, "Requesting /osc/state")

                override fun onSuccessful(objectResult: Any?) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    infoText = objectResult?.toString()
                    emitResult()
                }

                override fun onError(message: String) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent(message))
                }
            })
        }
    }

    fun getSupportOptions() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            val content = "{\"name\":\"camera.getOptions\", \"parameters\": {\"optionNames\": [\"_batteryCapacity\", \"remainingSpace\", \"totalSpace\", \"captureModeSupport\", \"captureIntervalSupport\", \"exposureProgramSupport\", \"isoSupport\", \"shutterSpeedSupport\", \"whiteBalanceSupport\", \"hdrSupport\", \"exposureBracketSupport\"]}}"
            oscManager.customRequest("/osc/commands/execute", content, object : IOscCallback {
                override fun onStartRequest() = emitLoading(true, "Requesting support options")

                override fun onSuccessful(objectResult: Any?) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    infoText = objectResult?.toString()
                    emitResult()
                }

                override fun onError(message: String) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent(message))
                }
            })
        }
    }

    fun takePicture() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            val options = "\"captureMode\":\"image\",\"hdr\":\"hdr\",\"exposureBracket\":{\"shots\":3,\"increment\":2}"
            oscManager.takePicture(options, createCaptureCallback())
        }
    }

    fun takePictureDeviceStitching() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            val options = "\"captureMode\":\"image\",\"hdr\":\"hdr\",\"photoStitching\":\"ondevice\""
            oscManager.takePicture(options, createCaptureCallback())
        }
    }

    fun startRecord() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            val options = "\"captureMode\":\"video\""
            oscManager.startRecord(options, object : IOscCallback {
                override fun onStartRequest() = emitLoading(true, "Starting record")

                override fun onSuccessful(objectResult: Any?) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent("start record success"))
                }

                override fun onError(message: String) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent(message))
                }
            })
        }
    }

    fun stopRecord() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            oscManager.stopRecord(createCaptureCallback())
        }
    }

    fun getExposureParams() {
        if (!ensureConnected()) return
        runOnCameraNetwork {
            oscManager.getCaptureExposureParamsForX2(object : IOscCallback {
                override fun onStartRequest() = emitLoading(true, "Getting exposure params")

                override fun onSuccessful(objectResult: Any?) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    val data = objectResult as? CaptureExposureData
                    if (data != null) {
                        data.exposureProgram = 1
                        captureExposureData = data
                        // 清除上一次的数据
                        frontSensorCapturePath = null
                        rearSensorCapturePath = null
                        stitchOutputPath = null
                        emitResult()
                    }
                }

                override fun onError(message: String) {
                    releaseCameraNetwork()
                    emitLoading(false)
                    emitEvent(OscProtocolEvent.ToastEvent(message))
                }
            })
        }
    }

    fun captureFrontSensor() {
        if (!ensureConnected()) return
        val exposureData = captureExposureData
        if (exposureData == null) {
            emitEvent(OscProtocolEvent.ToastEvent("请先获取曝光参数"))
            return
        }
        runOnCameraNetwork {
            frontCapturePending = true
            rearCapturePending = false
            oscManager.takeSingleSensorPictureForX2(1, exposureData, createCaptureCallback())
        }
    }

    fun captureRearSensor() {
        if (!ensureConnected()) return
        val exposureData = captureExposureData
        if (exposureData == null) {
            emitEvent(OscProtocolEvent.ToastEvent("请先获取曝光参数"))
            return
        }
        runOnCameraNetwork {
            rearCapturePending = true
            frontCapturePending = false
            oscManager.takeSingleSensorPictureForX2(2, exposureData, createCaptureCallback())
        }
    }

    fun separatedFisheyeStitch() {
        if (captureExposureData == null) {
            emitEvent(OscProtocolEvent.ToastEvent("请先获取曝光参数"))
            return
        }
        val frontPath = frontSensorCapturePath
        if (frontPath.isNullOrEmpty() || !File(frontPath).exists()) {
            emitEvent(OscProtocolEvent.ToastEvent("请先完成前镜头拍摄"))
            return
        }
        val rearPath = rearSensorCapturePath
        if (rearPath.isNullOrEmpty() || !File(rearPath).exists()) {
            emitEvent(OscProtocolEvent.ToastEvent("请先完成后镜头拍摄"))
            return
        }
        viewModelScope.launch {
            emitLoading(true, "Stitching...")
            val outputPath = withContext(Dispatchers.IO) {
                val outputDir = File("${StorageUtils.workDir}/osc")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val output = "${outputDir.absolutePath}/separated_fisheye_${System.currentTimeMillis()}.jpg"
                val result = StitchUtils.stitchSeparatedFisheyeFile(
                    WorkWrapper(arrayOf(frontPath, rearPath)),
                    output
                )
                if (result == 0) output else null
            }
            emitLoading(false)
            if (outputPath == null) {
                emitEvent(OscProtocolEvent.ToastEvent("拼接失败"))
            } else {
                stitchOutputPath = outputPath
                emitResult(needPlay = true)
            }
        }
    }

    private fun createCaptureCallback(): IOscCallback {
        return object : IOscCallback {
            override fun onStartRequest() = emitLoading(true, "Sending OSC request")

            override fun onSuccessful(objectResult: Any?) {
                releaseCameraNetwork()
                emitLoading(false)
                if (objectResult == null) {
                    frontCapturePending = false
                    rearCapturePending = false
                    return
                }
                val paths = objectResult as? Array<*>
                if (paths == null) {
                    infoText = objectResult.toString()
                    emitResult()
                    return
                }
                viewModelScope.launch {
                    val urls = paths.mapNotNull { it?.toString() }.toTypedArray()
                    val localPaths = downloadFiles(urls)
                    if (localPaths.isEmpty()) {
                        emitEvent(OscProtocolEvent.ToastEvent("下载文件失败"))
                    } else if (frontCapturePending) {
                        frontSensorCapturePath = localPaths.first()
                    } else if (rearCapturePending) {
                        rearSensorCapturePath = localPaths.first()
                    } else {
                        infoText = localPaths.joinToString("\n")
                    }
                    frontCapturePending = false
                    rearCapturePending = false
                    emitResult()
                }
            }

            override fun onError(message: String) {
                releaseCameraNetwork()
                emitLoading(false)
                frontCapturePending = false
                rearCapturePending = false
                emitEvent(OscProtocolEvent.ToastEvent(message))
            }
        }
    }

    private suspend fun downloadFiles(urls: Array<String>): List<String> {
        val saveDir = File("${StorageUtils.workDir}/osc")
        if (!saveDir.exists()) {
            saveDir.mkdirs()
        }
        val result = mutableListOf<String>()
        emitLoading(true, "Downloading files...")
        for (url in urls) {
            val localPath = downloadFile(url, saveDir.absolutePath) ?: run {
                emitLoading(false)
                return emptyList()
            }
            result.add(localPath)
        }
        emitLoading(false)
        return result
    }

    private suspend fun downloadFile(url: String, saveDir: String): String? {
        NetworkManager.cameraNet?.let { connectivityManager.bindProcessToNetwork(it) } ?: return null
        return suspendCancellableCoroutine { continuation ->
            val fileName = url.substring(url.lastIndexOf("/") + 1)
            var resumed = false
            OkGo.get<File>(url).execute(object : FileCallback(saveDir, fileName) {
                override fun onSuccess(response: Response<File>?) {
                    if (!resumed) {
                        resumed = true
                        continuation.resume(response?.body()?.absolutePath)
                    }
                }

                override fun onError(response: Response<File>?) {
                    if (!resumed) {
                        resumed = true
                        continuation.resume(null)
                    }
                }

                override fun onFinish() {
                    super.onFinish()
                    connectivityManager.bindProcessToNetwork(null)
                }
            })
        }
    }

    private fun emitLoading(show: Boolean, message: String = "") {
        emitEvent(OscProtocolEvent.LoadingEvent(show, message))
    }

    private fun emitResult(needPlay: Boolean = false) {
        emitEvent(
            OscProtocolEvent.ResultEvent(
                infoText = infoText,
                exposureText = captureExposureData?.toString(),
                frontPath = frontSensorCapturePath,
                rearPath = rearSensorCapturePath,
                stitchPath = stitchOutputPath,
                needPlay = needPlay
            )
        )
    }

    private fun runOnCameraNetwork(action: () -> Unit) {
        NetworkManager.cameraNet?.let { connectivityManager.bindProcessToNetwork(it) }
        action()
    }

    private fun releaseCameraNetwork() {
        connectivityManager.bindProcessToNetwork(null)
    }

    private fun ensureConnected(): Boolean {
        val connected = instaCameraManager.cameraConnectedType != InstaCameraManager.CONNECT_TYPE_NONE
        if (!connected) {
            emitEvent(OscProtocolEvent.ToastEvent("请先连接相机"))
        }
        return connected
    }
}