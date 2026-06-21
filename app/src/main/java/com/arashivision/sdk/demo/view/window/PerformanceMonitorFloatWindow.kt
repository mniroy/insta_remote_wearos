package com.arashivision.sdk.demo.view.window

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.arashivision.sdk.demo.databinding.WindowPerformanceMonitorBinding
import com.arashivision.sdk.demo.ext.roundTo
import com.arashivision.sdk.demo.util.PerformanceMonitor
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog

@SuppressLint("SetTextI18n")
class PerformanceMonitorFloatWindow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val logger: Logger = XLog.tag(PerformanceMonitorFloatWindow::class.java.simpleName).build()

    // 定时器相关
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            // 每秒执行的任务（这里示例：更新内容文本显示倒计时/计数）
            executePerSecondTask()

            // 循环执行：延迟1秒后再次执行自身
            mainHandler.postDelayed(this, 1500)
        }
    }

    val performanceMonitor = PerformanceMonitor()

    val binding: WindowPerformanceMonitorBinding = WindowPerformanceMonitorBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.tvCpuUsageValue.text = "${0}%"
        binding.tvDmipsValue.text = "0"
        binding.tvRamUsageValue.text = "${0}MB"
    }

    /**
     * 每秒执行的任务（核心业务逻辑，可根据需求修改）
     */
    private fun executePerSecondTask() {
        val metrics = performanceMonitor.getRealTimeMetrics()
        val dmips = metrics.processDmips
        val cpu = metrics.cpuUsagePercentage
        val ram = performanceMonitor.getProcessMemoryMb()
        logger.d("${cpu}% - ${dmips}DMIPS - ${ram}MB")
        binding.tvCpuUsageValue.text = "${cpu.roundTo(1)}%"
        binding.tvDmipsValue.text = "${dmips.roundTo(1)}"
        binding.tvRamUsageValue.text = "${ram.roundTo(1)}MB"
    }

    /**
     * 启动定时器
     */
    private fun startTimer() {
        // 先停止避免重复启动
        stopTimer()
        // 立即执行一次，然后每秒执行
        mainHandler.postDelayed(timerRunnable, 100)
    }

    /**
     * 停止定时器
     */
    private fun stopTimer() {
        mainHandler.removeCallbacks(timerRunnable)
    }

    /**
     * View挂载到窗口时（比如Activity启动）
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 如果View此时是显示状态，启动定时器
        startTimer()
    }

    /**
     * View从窗口卸载时（比如Activity销毁）
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 强制停止定时器，防止内存泄漏
        stopTimer()
        // 移除所有Handler回调，彻底清理
        mainHandler.removeCallbacksAndMessages(null)
    }
}