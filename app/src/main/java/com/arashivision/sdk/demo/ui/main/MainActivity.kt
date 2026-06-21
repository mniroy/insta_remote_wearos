package com.arashivision.sdk.demo.ui.main

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.arashivision.sdk.demo.InstaApp
import com.arashivision.sdk.demo.R
import com.arashivision.sdk.demo.base.BaseActivity
import com.arashivision.sdk.demo.base.BaseEvent
import com.arashivision.sdk.demo.databinding.ActivityMainBinding
import com.arashivision.sdk.demo.pref.Pref
import com.arashivision.sdk.demo.ui.ability.AbilityFragment
import com.arashivision.sdk.demo.ui.album.AlbumFragment
import com.arashivision.sdk.demo.ui.connect.ConnectFragment
import com.arashivision.sdk.demo.ui.main.MainEvent.PermissionDeniedEvent
import com.arashivision.sdk.demo.ui.main.MainEvent.PermissionGrantedEvent
import com.arashivision.sdk.demo.ui.setting.SettingFragment
import com.arashivision.sdk.demo.view.window.FloatWindowManager
import com.arashivision.sdk.demo.view.window.PerformanceMonitorFloatWindow

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    private val mFragments: MutableList<Fragment> = ArrayList()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.checkPermission(this)
    }

    override fun initView() {
        super.initView()
        mFragments.add(ConnectFragment())
        mFragments.add(AlbumFragment())
        mFragments.add(AbilityFragment())
        mFragments.add(SettingFragment())

        // 默认显示第一个Fragment
        checkFragment(0)

        if (Pref.getPerformanceMonitor()) openPerformanceMonitorWindow()
    }

    public override fun initListener() {
        super.initListener()
        binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_album -> checkFragment(1)
                R.id.nav_ability -> checkFragment(2)
                R.id.nav_setting -> checkFragment(3)
                else -> checkFragment(0)
            }

        }
    }

    public override fun onEvent(event: BaseEvent) {
        super.onEvent(event)

        if (event is PermissionGrantedEvent) {
            toast(R.string.toast_permission_request_complete)
        } else if (event is PermissionDeniedEvent) {
            toast(R.string.toast_permission_request_failed, true)
        }
    }

    fun checkFragment(index: Int): Boolean {
        if (index < 0 || index >= mFragments.size) return false
        val fragment = mFragments[index]
        val supportFragmentManager = supportFragmentManager
        val transaction = supportFragmentManager.beginTransaction()
        for (f in supportFragmentManager.fragments) {
            transaction.hide(f)
        }
        if (!supportFragmentManager.fragments.contains(fragment)) {
            transaction.add(R.id.fragment_container, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()
        return true
    }

    val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
        override fun onActivityStarted(activity: android.app.Activity) {}
        override fun onActivityResumed(activity: android.app.Activity) {
            FloatWindowManager.getInstance().showFloatWindow()
        }

        override fun onActivityPaused(activity: android.app.Activity) {
            FloatWindowManager.getInstance().hideFloatWindow()
        }

        override fun onActivityStopped(activity: android.app.Activity) {}
        override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
        override fun onActivityDestroyed(activity: android.app.Activity) {}
    }

    fun openPerformanceMonitorWindow() {
        if (viewModel.hasWindowPermission(this)) {
            showPerformanceMonitorWindow()
        } else {
            viewModel.checkWindowPermission(this) {
                if (it) {
                    toast(R.string.window_permission_request_success)
                    showPerformanceMonitorWindow()
                } else {
                    toast(R.string.window_permission_request_failed)
                }
            }
        }
    }

    private fun showPerformanceMonitorWindow() {
        InstaApp.instance.registerActivityLifecycleCallbacks(callback)
        FloatWindowManager.getInstance().setFloatView(PerformanceMonitorFloatWindow(this), canDrag = true)
        // 显示悬浮窗
        FloatWindowManager.getInstance().showFloatWindow()
    }

    fun closePerformanceMonitorWindow() {
        InstaApp.instance.unregisterActivityLifecycleCallbacks(callback)
        // 显示悬浮窗
        FloatWindowManager.getInstance().hideFloatWindow()
        FloatWindowManager.getInstance().setFloatView(null, canDrag = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (Settings.canDrawOverlays(this)) {
                openPerformanceMonitorWindow()
            }
        }
    }
}