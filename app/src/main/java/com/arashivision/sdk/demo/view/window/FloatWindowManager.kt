package com.arashivision.sdk.demo.view.window

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.arashivision.sdk.demo.InstaApp

class FloatWindowManager private constructor(private val application: Application) {

    // WindowManager 实例
    private val windowManager by lazy {
        application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    // 悬浮窗布局参数（逻辑不变）
    private val floatViewParams by lazy {
        WindowManager.LayoutParams().apply {
            type =
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 100
            y = 200
        }
    }

    // 悬浮窗视图（改为可外部设置）
    private var floatView: View? = null

    // 触摸事件相关变量
    private var lastX = 0
    private var lastY = 0
    private var downX = 0f
    private var downY = 0f

    /**
     * 外部设置悬浮窗视图
     * @param view 自定义的悬浮窗视图
     * @param canDrag 是否允许拖动（默认允许）
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setFloatView(view: View?, canDrag: Boolean = true) {
        this.floatView = view
        // 如果允许拖动，为视图添加触摸事件
        if (canDrag) {
            view?.setOnTouchListener { v, event ->
                handleTouchEvent(event)
            }
        }
    }

    /**
     * 处理触摸事件，实现拖动逻辑
     */
    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = floatViewParams.x
                lastY = floatViewParams.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - downX).toInt()
                val dy = (event.rawY - downY).toInt()
                floatViewParams.x = lastX + dx
                floatViewParams.y = lastY + dy
                windowManager.updateViewLayout(floatView, floatViewParams)
                return true
            }

            else -> return false
        }
    }

    /**
     * 显示悬浮窗（增加非空判断）
     */
    @SuppressLint("MissingPermission")
    fun showFloatWindow() {
        try {
            val view = floatView ?: return // 未设置视图则直接返回
            if (view.parent == null) {
                windowManager.addView(view, floatViewParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 隐藏悬浮窗
     */
    fun hideFloatWindow() {
        try {
            val view = floatView ?: return
            if (view.parent != null) {
                windowManager.removeView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 单例模式（逻辑不变）
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: FloatWindowManager? = null

        fun getInstance(): FloatWindowManager {
            return instance ?: synchronized(this) {
                instance ?: FloatWindowManager(InstaApp.instance).also { instance = it }
            }
        }
    }
}