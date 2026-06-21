package com.arashivision.sdk.demo.ui.osc

import android.text.BoringLayout
import com.arashivision.sdk.demo.base.BaseEvent

open class OscProtocolEvent : BaseEvent {
    class LoadingEvent(val show: Boolean, val message: String = "") : OscProtocolEvent()
    class ToastEvent(val message: String) : OscProtocolEvent()
    class LayoutFeatureEvent(val visible: Boolean) : OscProtocolEvent()
    class ResultEvent(
        val infoText: String? = null,
        val exposureText: String? = null,
        val frontPath: String? = null,
        val rearPath: String? = null,
        val stitchPath: String? = null,
        val needPlay: Boolean = false,
    ) : OscProtocolEvent()
}
