package com.arashivision.sdk.demo.ui.capture

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.arashivision.graphicpath.insmedia.common.MediaFrame
import com.arashivision.insta360.basemedia.ui.player.capture.IMediaFrameCallback
import com.arashivision.sdk.demo.R
import com.arashivision.sdk.demo.base.BaseActivity
import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.base.EventStatus.FAILED
import com.arashivision.sdk.demo.base.EventStatus.PROGRESS
import com.arashivision.sdk.demo.base.EventStatus.START
import com.arashivision.sdk.demo.base.EventStatus.SUCCESS
import com.arashivision.sdk.demo.databinding.ActivityCaptureBinding
import com.arashivision.sdk.demo.ext.durationFormat
import com.arashivision.sdk.demo.ext.gb
import com.arashivision.sdk.demo.ext.instaCameraManager
import com.arashivision.sdk.demo.ext.vibrate
import com.arashivision.sdk.demo.pref.Pref
import com.arashivision.sdk.demo.ui.capture.adapter.CaptureModeAdapter
import com.arashivision.sdk.demo.view.CaptureShutterButton
import com.arashivision.sdk.demo.view.discretescrollview.DSVOrientation
import com.arashivision.sdk.demo.view.discretescrollview.FadingEdgeDecoration
import com.arashivision.sdk.demo.view.discretescrollview.transform.ScaleTransformer
import com.arashivision.sdk.demo.view.picker.PickData
import com.arashivision.sdkcamera.camera.model.CaptureMode
import com.arashivision.sdkcamera.camera.model.CaptureSetting
import com.arashivision.sdkmedia.player.listener.PlayerViewListener
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog

class CaptureActivity : BaseActivity<ActivityCaptureBinding, CaptureViewModel>() {

    private val logger: Logger = XLog.tag(CaptureActivity::class.java.simpleName).build()

    private var captureModeAdapter: CaptureModeAdapter? = null

    override fun onStop() {
        super.onStop()
        if (isFinishing) viewModel.closePreviewStream()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        super.initView()
        binding.capturePlayerView.setLifecycle(this.lifecycle)

        binding.svCaptureMode.setSlideOnFling(true)
        captureModeAdapter = CaptureModeAdapter()
        binding.svCaptureMode.setAdapter(captureModeAdapter)
        binding.svCaptureMode.setOrientation(DSVOrientation.HORIZONTAL)
        binding.svCaptureMode.setOverScrollEnabled(true)
        binding.svCaptureMode.setSlideOnFling(true)
        binding.svCaptureMode.setSlideOnFlingThreshold(1300)
        binding.svCaptureMode.setItemTransitionTimeMillis(180)

        binding.svCaptureMode.setItemTransformer(
            ScaleTransformer.Builder().setMinScale(0.8f).build()
        )

        binding.svCaptureMode.addItemDecoration(FadingEdgeDecoration())

        binding.pickCaptureSetting.setTitleText(getString(R.string.capture_settings))

        if (Pref.getCustomSurface()) {
            binding.surfaceView.visibility = View.VISIBLE
        } else {
            binding.surfaceView.visibility = View.GONE
        }

        viewModel.initCapture()
    }

    override fun initListener() {
        super.initListener()
        binding.btnCapture.setOnClickListener { viewModel.startCapture() }

        binding.ivCaptureSetting.setOnClickListener { showCaptureSettingView() }

        binding.svCaptureMode.addOnItemChangedListener { _: RecyclerView.ViewHolder?, position: Int ->
            vibrate(50, 10)
            viewModel.switchCaptureMode(position)
        }

        captureModeAdapter?.setItemClickListener { _: String, position: Int ->
            binding.svCaptureMode.smoothScrollToPosition(position)
        }

        binding.pickCaptureSetting.setOnItemClickListener { position, data ->
            val supportCaptureSettingList: List<CaptureSetting> = viewModel.cameraOfflineData.let {
                instaCameraManager.getSupportCaptureSettingList(it.currentCaptureMode)
            }
            val captureSetting: CaptureSetting = supportCaptureSettingList[position]
            viewModel.cameraOfflineData.setCaptureSetting(captureSetting, data) {
                binding.pickCaptureSetting.setData(captureSettingDataList)
            }
            binding.pickCaptureSetting.setData(captureSettingDataList)

            // 更新SD卡余量
            viewModel.updateSDRemaining(true)
        }

        binding.ivCaptureHighlight.setOnClickListener {
            instaCameraManager.markHighlightMoments()
            toast(R.string.capture_marked)
        }
    }

    private val captureSettingDataList: List<PickData>
        get() {
            val supportCaptureSettingList: List<CaptureSetting> = viewModel.cameraOfflineData.let {
                instaCameraManager.getSupportCaptureSettingList(it.currentCaptureMode)
            }

            return supportCaptureSettingList.map { getCaptureSettingData(it) }
        }

    private fun getCaptureSettingData(captureSetting: CaptureSetting): PickData {
        val title = getString(getCaptureSettingNameResId(captureSetting))
        val captureSettingValue = viewModel.cameraOfflineData.getCaptureSetting(captureSetting)
        val captureSettingSupportList = viewModel.getCaptureSettingSupportValueList(captureSetting)
        val index: Int = captureSettingSupportList.indexOfFirst { captureSettingValue == it }.coerceAtLeast(0)
        val options = captureSettingSupportList.map { value -> getCaptureSettingValueName(this, captureSetting, value) to value }
        return PickData(true, title, index, options)
    }

    private fun showCaptureSettingView() {
        binding.pickCaptureSetting.setData(captureSettingDataList)
        binding.pickCaptureSetting.show()
    }

    @SuppressLint("SetTextI18n")
    override fun onEvent(event: BaseEvent) {
        super.onEvent(event)
        when (event) {
            // Wi-Fi断连事件
            is CaptureEvent.CameraWiFiDisconnectEvent -> finish()

            // 拍摄页面初始化
            is CaptureEvent.InitCaptureEvent -> {
                logger.d("event.status=${event.status}   event.step=${event.step}")
                when (event.status) {
                    START -> showLoading()

                    PROGRESS -> stepToLoadingTextMap[event.step]?.let { showLoading(it) }

                    SUCCESS -> {
                        showLoading(R.string.capture_rendering_player)
                        displayPreviewStream()
                        updateCaptureModeUi(event.captureModeList, event.currentCaptureMode)
                        binding.btnCapture.setState(if (event.isSingleClickAction == true) CaptureShutterButton.State.CAPTURE_IDLE else CaptureShutterButton.State.RECORD_IDLE)
                    }

                    FAILED -> {
                        hideLoading()
                        stepToErrorTextMap[event.step]?.let { lastToast(it) }
                        finish()
                    }
                }
            }
            // 主动切换相机模式事件
            is CaptureEvent.SwitchCaptureModeEvent -> {
                when (event.status) {
                    START -> showLoading(R.string.capture_mode_switching)
                    SUCCESS -> {
                        hideLoading()
                        binding.btnCapture.setState(if (event.isSingleClickAction == true) CaptureShutterButton.State.CAPTURE_IDLE else CaptureShutterButton.State.RECORD_IDLE)
                    }

                    FAILED -> {
                        hideLoading()
                        toast(R.string.capture_mode_switch_failed)
                    }

                    else -> {}
                }
            }

            // 新拍摄流程预览流参数变化通知
            is CaptureEvent.CameraPreviewStreamParamsChangedEvent -> {
                viewModel.cameraPreviewStreamParamsChanged(binding.capturePlayerView)
            }

            // 重启播放器
            is CaptureEvent.RestartPlayerViewEvent -> replay(event.onFinish)

            // 更新播放器参数，但不重启
            is CaptureEvent.UpdatePlayerViewParamsEvent -> {
                if (event.offsetData != null && event.stabOffset != null) {
                    binding.capturePlayerView.setOffset(event.offsetData, event.stabOffset)
                }
                if (event.windowCropInfo != null) {
                    binding.capturePlayerView.windowCropInfo = event.windowCropInfo
                }
                event.streamResolution?.apply {
                    binding.capturePlayerView.setPreviewResolution(width, height, fps)
                }
            }

            // 拍摄事件
            is CaptureEvent.CameraCaptureEvent -> {
                logger.d("  status=${event.status}")
                when (event.status) {
                    CaptureEvent.CaptureStatus.SD_DISABLE -> toast(R.string.toast_no_sd)
                    CaptureEvent.CaptureStatus.STARTING -> {
                        binding.notSupportPreview.visibility = if (!viewModel.supportPreview()) View.VISIBLE else View.GONE
                        binding.ivCaptureHighlight.visibility = if (event.captureMode.isVideoMode) View.VISIBLE else View.GONE
                        showLoading(R.string.capture_preparing)
                    }

                    CaptureEvent.CaptureStatus.STOPPING -> showLoading(R.string.capture_stopping)
                    CaptureEvent.CaptureStatus.WORKING -> {
                        hideLoading()
                        binding.ivCaptureSetting.visibility = View.GONE
                        binding.svCaptureMode.visibility = View.INVISIBLE
                        binding.btnCapture.setState(if (event.isSingleClickAction) CaptureShutterButton.State.CAPTURING else CaptureShutterButton.State.RECORDING)
                    }

                    CaptureEvent.CaptureStatus.FINISH -> {
                        binding.notSupportPreview.visibility = View.GONE
                        binding.ivCaptureHighlight.visibility = View.GONE
                        captureComplete(event.isSingleClickAction)
                    }

                    CaptureEvent.CaptureStatus.RECORD_TIME -> {
                        binding.tvRecordTime.visibility = View.VISIBLE
                        binding.tvRecordTime.text = event.recordTime.durationFormat()
                        if (event.videoTime != -1L) {
                            binding.tvVideoDuration.visibility = View.VISIBLE
                            binding.ivArrow.visibility = View.VISIBLE
                            binding.tvVideoDuration.text = event.videoTime.durationFormat()
                        }
                    }

                    CaptureEvent.CaptureStatus.CAPTURE_COUNT -> {
                        binding.tvRecordTime.visibility = View.VISIBLE
                        binding.tvRecordTime.text = getString(R.string.capture_count, event.captureCount)
                    }

                    CaptureEvent.CaptureStatus.ERROR -> {
                        captureComplete(event.isSingleClickAction)
                        toast(getString(R.string.capture_error, event.errorCode))
                    }
                }
            }

            // 直播事件
            is CaptureEvent.CameraLiveEvent -> {
                when (event.status) {
                    CaptureEvent.LiveStatus.RTMP_EMPTY -> toast(R.string.capture_live_rtmp_empty)

                    CaptureEvent.LiveStatus.START_LIVE -> {
                        binding.ivCaptureSetting.visibility = View.GONE
                        showLoading(R.string.capture_live_starting)
                    }

                    CaptureEvent.LiveStatus.STOP_LIVE -> showLoading(R.string.capture_live_closing)

                    CaptureEvent.LiveStatus.PUSH_STARTED -> {
                        hideLoading()
                        binding.btnCapture.setState(CaptureShutterButton.State.RECORDING)
                        toast(R.string.capture_live_start_push)
                    }

                    CaptureEvent.LiveStatus.PUSH_FINISHED -> {
                        hideLoading()
                        binding.ivCaptureSetting.visibility = View.VISIBLE
                        binding.btnCapture.setState(CaptureShutterButton.State.RECORD_IDLE)
                    }

                    CaptureEvent.LiveStatus.PUSH_ERROR -> {
                        hideLoading()
                        binding.btnCapture.setState(CaptureShutterButton.State.RECORD_IDLE)
                        toast(R.string.capture_live_push_error)
                    }

                }
            }

            // 预览流关闭事件
            CaptureEvent.CameraPreviewStreamClosed -> {
                // Preview Stopped
                binding.capturePlayerView.destroy()
                binding.capturePlayerView.keepScreenOn = false
            }

            // 更新剩余可录制时间、可拍摄照片数
            is CaptureEvent.UpdateRemainingEvent -> {
                if (event.captureMode.isLiveMode) {
                    return
                }
                binding.tvRemaining.visibility = View.GONE
                if (event.remaining < 0) {
                    binding.tvRemaining.text = getString(R.string.text_camera_sd_free) + event.freeSpace.gb()
                } else if (event.captureMode.isPhotoMode) {
                    binding.tvRemaining.text = getString(R.string.capture_remaining_take_photo, "" + event.remaining)
                } else if (event.captureMode.isVideoMode) {
                    binding.tvRemaining.visibility = View.VISIBLE
                    binding.tvRemaining.text = getString(R.string.capture_remaining_record_video, (event.remaining * 1000L).durationFormat())
                }
            }

            CaptureEvent.ReopenPreviewStreamEvent -> showLoading(R.string.capture_reopen_preview_stream)
        }
    }

    private fun captureComplete(isSingleClickAction: Boolean) {
        hideLoading()
        binding.ivCaptureSetting.visibility = View.VISIBLE
        binding.svCaptureMode.visibility = View.VISIBLE
        binding.tvRecordTime.visibility = View.GONE
        binding.tvVideoDuration.visibility = View.GONE
        binding.ivArrow.setVisibility(View.GONE)
        binding.btnCapture.setState(if (isSingleClickAction) CaptureShutterButton.State.CAPTURE_IDLE else CaptureShutterButton.State.RECORD_IDLE)
    }

    private fun replay(onFinish: (() -> Unit)? = null) {
        if (isFinishing || isDestroyed || !viewModel.supportPreview()) return
        showLoading(R.string.capture_reopen_capture_player_view)
        this.onFinish = onFinish
        binding.capturePlayerView.prepare(viewModel.getCaptureParams())
        binding.capturePlayerView.play()
    }

    private var onFinish: (() -> Unit)? = null

    private fun displayPreviewStream() {
        binding.capturePlayerView.setPlayerViewListener(object : PlayerViewListener {
            override fun onFirstFrameRender() {
                logger.d("capturePlayerView  onFirstFrameRender")
                hideLoading()
            }

            override fun onLoadingFinish() {
                logger.d("capturePlayerView  onLoadingFinish")
                instaCameraManager.setPipeline(binding.capturePlayerView.pipeline)
                this@CaptureActivity.onFinish?.invoke()  // 直播需要在设置 pipeline 之后开启
                this@CaptureActivity.onFinish = null
                hideLoading()
                if (Pref.getCustomSurface()) {
                    // 获取拼接后的数据，渲染到SurfaceView上面
                    binding.capturePlayerView.startExtractMediaFrame(binding.surfaceView.width, binding.surfaceView.height, 60, 32, IMediaFrameCallback { mediaFrame: MediaFrame? ->
                        binding.surfaceView.updateArgbBuffer(mediaFrame!!.planes[0], mediaFrame.width, mediaFrame.height)
                    })
                }
            }

            override fun onReleaseCameraPipeline() {
                logger.d("capturePlayerView  onReleaseCameraPipeline")
                instaCameraManager.setPipeline(null)
            }

        })

        binding.capturePlayerView.post {
            binding.capturePlayerView.prepare(viewModel.getCaptureParams().apply {
                width = binding.capturePlayerView.width
                height = binding.capturePlayerView.height
            })
            binding.capturePlayerView.play()
            binding.capturePlayerView.keepScreenOn = true
        }
    }

    private fun updateCaptureModeUi(captureModeList: List<CaptureMode>?, currentCaptureMode: CaptureMode?) {
        captureModeList?.takeIf { it.isNotEmpty() } ?: return
        currentCaptureMode?.takeIf { it in captureModeList } ?: return

        val data = captureModeList.mapNotNull { mode ->
            getCaptureModeTextResId(mode)?.let { getString(it) }
        }

        captureModeAdapter?.setData(data.toMutableList())
        binding.svCaptureMode.scrollToPosition(captureModeList.indexOf(currentCaptureMode))
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.pickCaptureSetting.isVisible) {
            binding.pickCaptureSetting.hide()
            return
        }
        if (!instaCameraManager.isCameraWorking) super.onBackPressed()
    }

    override fun onDestroy() {
        binding.capturePlayerView.destroy()
        super.onDestroy()
    }
}