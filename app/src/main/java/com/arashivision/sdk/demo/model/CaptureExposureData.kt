package com.arashivision.sdk.demo.model

class CaptureExposureData {
    var exposureProgram: Int = 0
    var iso: Int = 0
    var shutterSpeed: Double = 0.0
    var whiteBalance: String = ""
    var _WbRGain: Int = 0
    var _WbBGain: Int = 0

    override fun toString(): String {
        return "exposureProgram=$exposureProgram, iso=$iso, shutterSpeed=$shutterSpeed, whiteBalance=$whiteBalance, _WbRGain=$_WbRGain, _WbBGain=$_WbBGain"
    }
}
