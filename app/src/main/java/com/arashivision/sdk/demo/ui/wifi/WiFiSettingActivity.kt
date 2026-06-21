package com.arashivision.sdk.demo.ui.wifi

import android.os.Bundle
import com.arashivision.sdk.demo.R
import com.arashivision.sdk.demo.base.BaseActivity
import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.base.EventStatus
import com.arashivision.sdk.demo.databinding.ActivityWifiSettingBinding
import com.arashivision.sdk.demo.ui.wifi.adapter.WiFiChannelAdapter

class WiFiSettingActivity : BaseActivity<ActivityWifiSettingBinding, WiFiSettingViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initWifiSetting()
    }


    override fun initListener() {
        super.initListener()
        binding.tvApplyCountry.setOnClickListener {
            val countryCode = binding.etCountry.text.toString()
            if (countryCode.isEmpty()) {
                toast(R.string.wifi_setting_country_edit_hint)
                return@setOnClickListener
            }
            viewModel.setWiFiCountry(countryCode)
        }

        binding.tvRefreshWifiInfo.setOnClickListener {
            viewModel.initWifiSetting()
        }

        binding.tvOpenWifi.setOnClickListener {
            viewModel.openCameraWiFi()
        }

        binding.tvCloseWifi.setOnClickListener {
            viewModel.closeCameraWifi()
        }
    }

    override fun onEvent(event: BaseEvent) {
        super.onEvent(event)
        when (event) {
            
            is BaseEvent.CameraStatusChangedEvent -> if (!event.enable) finish()

            is WiFiSettingEvent.InitWifiSettingEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading()
                    EventStatus.SUCCESS -> {
                        updateUi(event.country!!, event.channelList!!, event.channel!!)
                        hideLoading()
                    }

                    EventStatus.FAILED -> {
                        finish()
                        lastToast(R.string.wifi_setting_wifi_info_fetch_failed)
                    }

                    else -> {}
                }
            }

            is WiFiSettingEvent.SetWiFiChannelEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading()
                    EventStatus.SUCCESS -> {
                        hideLoading()
                        toast(R.string.wifi_setting_wifi_channel_setup_successful)
                    }

                    EventStatus.FAILED -> {
                        hideLoading()
                        toast(R.string.wifi_setting_wifi_channel_setup_failed)
                    }

                    else -> {}
                }
            }

            is WiFiSettingEvent.SetWiFiCountryEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading()
                    EventStatus.SUCCESS -> {
                        hideLoading()
                        toast(R.string.wifi_setting_wifi_country_setup_successful)
                        // 此处必须重启Wi-Fi，才能获取正确的信道列表
                        viewModel.resetCameraWifi()
                    }

                    EventStatus.FAILED -> {
                        hideLoading()
                        toast(R.string.wifi_setting_wifi_country_setup_failed)
                    }

                    else -> {}
                }
            }

            is WiFiSettingEvent.OpenCameraWiFiEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading()
                    EventStatus.SUCCESS -> {
                        hideLoading()
                        toast(R.string.wifi_setting_open_wifi_successful)
                    }

                    EventStatus.FAILED -> {
                        hideLoading()
                        toast(R.string.wifi_setting_open_wifi_failed)
                    }

                    else -> {}
                }
            }

            is WiFiSettingEvent.CloseCameraWiFiEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading()
                    EventStatus.SUCCESS -> {
                        hideLoading()
                        toast(R.string.wifi_setting_close_wifi_successful)
                    }

                    EventStatus.FAILED -> {
                        hideLoading()
                        toast(R.string.wifi_setting_close_wifi_failed)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun updateUi(country: String, channelList: IntArray, channel: Int) {
        binding.tvWifiCountryValue.text = country
        binding.flowLayout.adapter = WiFiChannelAdapter(channelList.map { it.toString() }.toList())
        binding.flowLayout.adapter.setSelectedList(channelList.indexOf(channel))
        binding.flowLayout.setOnTagClickListener { _, position, _ ->
            viewModel.setWiFiChannel(channelList[position])
            false
        }
    }
}