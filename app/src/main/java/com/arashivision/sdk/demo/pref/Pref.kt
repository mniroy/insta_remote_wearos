package com.arashivision.sdk.demo.pref

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.arashivision.sdk.demo.InstaApp
import com.arashivision.sdk.demo.R

object Pref {

    fun getStabCacheFrameNum(): Int {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getString(InstaApp.instance.getString(R.string.pref_stab_cache_frame_num), "0")
            ?.toInt() ?: 0
    }

    fun setStabCacheFrameNum(num: Int) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putInt(
                InstaApp.instance.getString(R.string.pref_stab_cache_frame_num), num
            )
        }
    }


    fun getRealTimeCaptureLogs(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getBoolean(
            InstaApp.instance.getString(R.string.pref_real_time_capture_logs), true
        )
    }

    fun setRealTimeCaptureLogs(enable: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putBoolean(
                InstaApp.instance.getString(R.string.pref_real_time_capture_logs), enable
            )
        }
    }

    fun getLiveRtmp(): String {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getString(InstaApp.instance.getString(R.string.pref_live_rtmp), "") ?: ""
    }

    fun setLiveRtmp(rtmp: String) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putString(
                InstaApp.instance.getString(R.string.pref_live_rtmp), rtmp
            )
        }
    }

    fun getLiveBindMobileNetwork(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getBoolean(
            InstaApp.instance.getString(R.string.pref_live_bind_mobile_network),
            true
        )
    }

    fun setLiveBindMobileNetwork(enable: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putBoolean(
                InstaApp.instance.getString(R.string.pref_live_bind_mobile_network),
                enable
            )
        }
    }

    fun getCustomSurface(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getBoolean(InstaApp.instance.getString(R.string.pref_custom_surface), true)
    }

    fun setCustomSurface(enable: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putBoolean(
                InstaApp.instance.getString(R.string.pref_custom_surface),
                enable
            )
        }
    }

    fun getCaptureActivityPlaneStitch(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getBoolean(
            InstaApp.instance.getString(R.string.pref_capture_activity_plane_stitch),
            false
        )
    }

    fun setCaptureActivityPlaneStitch(enable: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putBoolean(
                InstaApp.instance.getString(R.string.pref_capture_activity_plane_stitch),
                enable
            )
        }
    }

    fun getPerformanceMonitor(): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.getBoolean(
            InstaApp.instance.getString(R.string.pref_performance_monitor),
            false
        )
    }

    fun setPerformanceMonitor(enable: Boolean) {
        val sp = PreferenceManager.getDefaultSharedPreferences(InstaApp.instance)
        return sp.edit(commit = true) {
            putBoolean(
                InstaApp.instance.getString(R.string.pref_performance_monitor),
                enable
            )
        }
    }

}