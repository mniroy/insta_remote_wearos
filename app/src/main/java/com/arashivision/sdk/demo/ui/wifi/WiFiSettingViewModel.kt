package com.arashivision.sdk.demo.ui.wifi

import com.arashivision.sdk.demo.base.BaseViewModel
import com.arashivision.sdk.demo.base.EventStatus
import com.arashivision.sdk.demo.ext.instaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback

class WiFiSettingViewModel : BaseViewModel() {

    fun initWifiSetting() {
        emitEvent(WiFiSettingEvent.InitWifiSettingEvent(EventStatus.START))
        instaCameraManager.fetchWifiChannel { s, i ->
            if (s.isNullOrEmpty() || i == 0) {
                emitEvent(WiFiSettingEvent.InitWifiSettingEvent(EventStatus.FAILED))
                return@fetchWifiChannel
            }
            emitEvent(
                WiFiSettingEvent.InitWifiSettingEvent(
                    EventStatus.SUCCESS,
                    s,
                    instaCameraManager.wifiChannelList,
                    i
                )
            )
        }
    }

    fun setWiFiChannel(channel: Int) {
        emitEvent(WiFiSettingEvent.SetWiFiChannelEvent(EventStatus.START))
        instaCameraManager.resetCameraWifi(channel, object : ICameraOperateCallback {
            override fun onSuccessful() {
                emitEvent(WiFiSettingEvent.SetWiFiChannelEvent(EventStatus.SUCCESS))
            }

            override fun onFailed() {
                emitEvent(WiFiSettingEvent.SetWiFiChannelEvent(EventStatus.FAILED))
            }

            override fun onCameraConnectError() {
                emitEvent(WiFiSettingEvent.SetWiFiChannelEvent(EventStatus.FAILED))
            }
        })
    }

    fun setWiFiCountry(countryCode: String) {
        emitEvent(WiFiSettingEvent.SetWiFiCountryEvent(EventStatus.START))
        instaCameraManager.setWifiCountry(countryCode, object : ICameraOperateCallback {
            override fun onSuccessful() {
                emitEvent(WiFiSettingEvent.SetWiFiCountryEvent(EventStatus.SUCCESS))
            }

            override fun onFailed() {
                emitEvent(WiFiSettingEvent.SetWiFiCountryEvent(EventStatus.FAILED))
            }

            override fun onCameraConnectError() {
                emitEvent(WiFiSettingEvent.SetWiFiCountryEvent(EventStatus.FAILED))
            }
        })
    }

    fun resetCameraWifi() {
        instaCameraManager.resetCameraWifi(object : ICameraOperateCallback {
            override fun onSuccessful() {}
            override fun onFailed() {}
            override fun onCameraConnectError() {}
        })
    }

    fun openCameraWiFi() {
        emitEvent(WiFiSettingEvent.OpenCameraWiFiEvent(EventStatus.START))
        instaCameraManager.openCameraWifi(object : ICameraOperateCallback {
            override fun onSuccessful() {
                emitEvent(WiFiSettingEvent.OpenCameraWiFiEvent(EventStatus.SUCCESS))
            }
            override fun onFailed() {
                emitEvent(WiFiSettingEvent.OpenCameraWiFiEvent(EventStatus.FAILED))
            }
            override fun onCameraConnectError() {
                emitEvent(WiFiSettingEvent.OpenCameraWiFiEvent(EventStatus.FAILED))
            }
        })
    }

    fun closeCameraWifi() {
        emitEvent(WiFiSettingEvent.CloseCameraWiFiEvent(EventStatus.START))
        instaCameraManager.closeCameraWifi(object : ICameraOperateCallback {
            override fun onSuccessful() {
                emitEvent(WiFiSettingEvent.CloseCameraWiFiEvent(EventStatus.SUCCESS))
            }
            override fun onFailed() {
                emitEvent(WiFiSettingEvent.CloseCameraWiFiEvent(EventStatus.FAILED))
            }
            override fun onCameraConnectError() {
                emitEvent(WiFiSettingEvent.CloseCameraWiFiEvent(EventStatus.FAILED))
            }
        })
    }
}