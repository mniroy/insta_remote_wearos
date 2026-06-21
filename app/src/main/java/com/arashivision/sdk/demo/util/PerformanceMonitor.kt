package com.arashivision.sdk.demo.util

import android.os.Debug


class PerformanceMonitor {

    // 传入迭代次数（建议 500,000 次，约耗时 5-10ms）
    private external fun nativeGetCombinedMetrics(iterations: Long): DoubleArray?

    private val totalCores = Runtime.getRuntime().availableProcessors()

    class Metrics {
        var cpuUsagePercentage: Double = 0.0 // 进程总占用率 (0-100%)
        var processDmips: Double = 0.0 // 当前进程消耗的真实 DMIPS
        var singleCoreLimit: Double = 0.0 // 当前环境单核物理极限能力
    }

    /**
     * 获取完整的实时性能指标
     */
    fun getRealTimeMetrics(): Metrics {
        // 执行 Native 采样
        val raw: DoubleArray = nativeGetCombinedMetrics(500000)!!

        val m = Metrics()
        // raw[0] 是消耗的核心当量 (例如 1.2 代表用了 1.2 个核)
        m.cpuUsagePercentage = (raw[0] / totalCores) * 100.0


        // raw[1] 是单核极限 DMIPS
        m.singleCoreLimit = raw[1]


        // 进程总 DMIPS = 消耗核心当量 * 单核极限能力
        m.processDmips = raw[0] * raw[1]

        return m
    }

    /**
     * 获取进程内存使用（MB）
     */
    fun getProcessMemoryMb(): Float {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        return memInfo.totalPss / 1024f
    }
}