package com.arashivision.sdk.demo.ui.wifi

import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.base.EventStatus

open class WiFiSettingEvent : BaseEvent {
    class InitWifiSettingEvent(
        val status: EventStatus,
        val country: String? = null,
        val channelList: IntArray? = null,
        val channel: Int? = null
    ) : WiFiSettingEvent()

    class SetWiFiChannelEvent(val status: EventStatus) : WiFiSettingEvent()

    class SetWiFiCountryEvent(val status: EventStatus) : WiFiSettingEvent()

    class OpenCameraWiFiEvent(val status: EventStatus) : WiFiSettingEvent()

    class CloseCameraWiFiEvent(val status: EventStatus) : WiFiSettingEvent()
}