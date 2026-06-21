package com.arashivision.sdk.demo.ui.osc

import android.view.View
import com.arashivision.sdk.demo.R
import com.arashivision.sdk.demo.base.BaseActivity
import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.databinding.ActivityOscProtocolBinding
import com.arashivision.sdk.demo.ui.play.WorkPlayActivity
import com.arashivision.sdkmedia.work.WorkWrapper

class OscProtocolActivity : BaseActivity<ActivityOscProtocolBinding, OscProtocolViewModel>() {

    override fun initView() {
        super.initView()
        viewModel.initOsc()
    }

    override fun initListener() {
        super.initListener()
        binding.btnInfo.setOnClickListener { viewModel.requestInfo() }
        binding.btnState.setOnClickListener { viewModel.requestState() }
        binding.btnGetSupportOptions.setOnClickListener { viewModel.getSupportOptions() }
        binding.btnTakePicture.setOnClickListener { viewModel.takePicture() }
        binding.btnTakePictureDeviceStitching.setOnClickListener { viewModel.takePictureDeviceStitching() }
        binding.btnStartRecord.setOnClickListener { viewModel.startRecord() }
        binding.btnStopRecord.setOnClickListener { viewModel.stopRecord() }
        binding.btnGetExposureParams.setOnClickListener { viewModel.getExposureParams() }
        binding.btnSwitchFrontSensorCapture.setOnClickListener { viewModel.captureFrontSensor() }
        binding.btnSwitchRearSensorCapture.setOnClickListener { viewModel.captureRearSensor() }
        binding.btnSeparatedFisheyeStitch.setOnClickListener { viewModel.separatedFisheyeStitch() }
    }

    override fun onEvent(event: BaseEvent) {
        super.onEvent(event)
        when (event) {
            is BaseEvent.CameraStatusChangedEvent -> if (!event.enable) finish()

            is OscProtocolEvent.LoadingEvent -> {
                if (event.show) showLoading(event.message) else hideLoading()
            }

            is OscProtocolEvent.ToastEvent -> toast(event.message)

            is OscProtocolEvent.ResultEvent -> {
                binding.tvExposureParams.visibility = if (event.exposureText.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.tvForntPath.visibility = if (event.frontPath.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.tvRearPath.visibility = if (event.rearPath.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.tvStitchPath.visibility = if (event.stitchPath.isNullOrBlank()) View.GONE else View.VISIBLE

                event.infoText?.let {
                    binding.tvInfo.text = it
                }

                event.exposureText?.let {
                    binding.tvExposureParams.text = getString(R.string.osc_exposure_params, it)
                }
                event.frontPath?.let {
                    binding.tvForntPath.text = getString(R.string.osc_front_path, it)
                }
                event.rearPath?.let {
                    binding.tvRearPath.text = getString(R.string.osc_rear_path, it)
                }
                event.stitchPath?.let {
                    binding.tvStitchPath.text = getString(R.string.osc_stitch_path, it)
                    if(event.needPlay) {
                        WorkPlayActivity.launch(
                            this@OscProtocolActivity,
                            WorkWrapper(arrayOf<String>(it))
                        )
                    }
                }
            }

            is OscProtocolEvent.LayoutFeatureEvent -> {
                binding.layoutSeparatedFisheyeStitch.visibility = if (event.visible) View.VISIBLE else View.GONE
            }
        }
    }
}
