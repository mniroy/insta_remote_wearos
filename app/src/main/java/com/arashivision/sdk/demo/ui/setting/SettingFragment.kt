package com.arashivision.sdk.demo.ui.setting

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.arashivision.sdk.demo.R
import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.base.BasePreferenceFragment
import com.arashivision.sdk.demo.base.EventStatus
import com.arashivision.sdk.demo.pref.Pref
import com.arashivision.sdk.demo.ui.main.MainActivity
import com.arashivision.sdk.demo.util.ShareUtils.shareFile
import com.arashivision.sdkcamera.log.LogManager

class SettingFragment : BasePreferenceFragment<SettingViewModel>() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_setting, rootKey)

        initStabCacheFrameNum()
        initRealTimeCaptureLogs()
        initExportLogs()
        initLiveRtmp()
        initLiveBindMobileNetwork()
        initCustomSurface()
        initPlaneStitch()
        initPerformanceMonitor()
    }


    private fun initLiveRtmp() {
        val key = getString(R.string.pref_live_rtmp)
        val editTextPreference = findPreference<EditTextPreference>(key)
        editTextPreference?.summary = Pref.getLiveRtmp()
        editTextPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            true
        }
    }

    private fun initLiveBindMobileNetwork() {
        val key = getString(R.string.pref_live_bind_mobile_network)
        val switchPreference = findPreference<SwitchPreference>(key)
        switchPreference?.isChecked = Pref.getLiveBindMobileNetwork()
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            true
        }
    }

    private fun initStabCacheFrameNum() {
        val key = getString(R.string.pref_stab_cache_frame_num)
        val editTextPreference = findPreference<EditTextPreference>(key)
        editTextPreference?.summary = Pref.getStabCacheFrameNum().toString()
        editTextPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }
        editTextPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.filters = arrayOf(InputFilter.LengthFilter(3)) // 最多3位数字
        }
    }

    private fun initRealTimeCaptureLogs() {
        val key = getString(R.string.pref_real_time_capture_logs)
        val switchPref = findPreference<SwitchPreference>(key)
        switchPref?.isChecked = Pref.getRealTimeCaptureLogs()
        switchPref?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                LogManager.instance.startLogDumper()
            } else {
                LogManager.instance.stopLogDumper()
            }
            true
        }
    }


    private fun initExportLogs() {
        val key = getString(R.string.pref_export_log)
        val preference = findPreference<Preference>(key)
        preference?.setOnPreferenceClickListener {
            activity?.let { viewModel.exportTodayLog(it) }
            true
        }
    }

    private fun initCustomSurface() {
        val key = getString(R.string.pref_custom_surface)
        val switchPreference = findPreference<SwitchPreference>(key)
        switchPreference?.isChecked = Pref.getCustomSurface()
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            true
        }
    }

    private fun initPlaneStitch() {
        val key = getString(R.string.pref_capture_activity_plane_stitch)
        val switchPreference = findPreference<SwitchPreference>(key)
        switchPreference?.isChecked = Pref.getCaptureActivityPlaneStitch()
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            true
        }
    }

    private fun initPerformanceMonitor() {
        val key = getString(R.string.pref_performance_monitor)
        val switchPreference = findPreference<SwitchPreference>(key)
        switchPreference?.isChecked = Pref.getPerformanceMonitor()
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            val fragmentActivity = requireActivity()
            if (fragmentActivity is MainActivity) {
                if (newValue as Boolean) {
                    fragmentActivity.openPerformanceMonitorWindow()
                } else {
                    fragmentActivity.closePerformanceMonitorWindow()
                }
            }
            true
        }
    }

    override fun onEvent(event: BaseEvent?) {
        super.onEvent(event)
        when (event) {
            is SettingEvent.ExportLogEvent -> {
                when (event.status) {
                    EventStatus.START -> showLoading(R.string.setting_exporting_log)
                    EventStatus.SUCCESS -> {
                        hideLoading()
                        // 调用分享工具类
                        activity?.let {
                            event.zipFile?.let { file ->
                                shareFile(it, file)
                            }
                        }
                    }

                    EventStatus.FAILED -> {
                        hideLoading()
                        toast(R.string.setting_export_log_failed)
                    }

                    else -> {}
                }
            }
        }
    }

}
