#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <android/log.h>

#define TAG "CpuNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// 静态全局变量，用于存储上一次采样的状态
static struct {
    long last_process_ticks = 0;
    struct timespec last_sample_time = {0, 0};
    long clock_ticks_per_sec = 0;
} g_cpu_context;

// 获取进程 CPU 滴答数 (utime + stime)
long get_process_ticks() {
    FILE* fp = fopen("/proc/self/stat", "r");
    if (!fp) return 0;
    long utime = 0, stime = 0;
    // 跳过前13个字段，读取第14(utime)和15(stime)
    if (fscanf(fp, "%*d %*s %*c %*d %*d %*d %*d %*d %*u %*u %*u %*u %*u %ld %ld", &utime, &stime) != 2) {
        fclose(fp);
        return 0;
    }
    fclose(fp);
    return utime + stime;
}

extern "C" {

/**
 * 核心方法：同时返回 [进程占用核心数, 单核当前物理DMIPS]
 * 返回 jdoubleArray 减少多次 JNI 调用
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_arashivision_sdk_demo_util_PerformanceMonitor_nativeGetCombinedMetrics(JNIEnv *env, jobject thiz, jlong iterations) {
    if (g_cpu_context.clock_ticks_per_sec == 0) {
        g_cpu_context.clock_ticks_per_sec = sysconf(_SC_CLK_TCK);
    }

    // --- 第一部分：计算 CPU 占用率 ---
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    long current_ticks = get_process_ticks();

    double usage_cores = 0.0;
    if (g_cpu_context.last_sample_time.tv_sec != 0) {
        double time_diff = (now.tv_sec - g_cpu_context.last_sample_time.tv_sec) +
                           (now.tv_nsec - g_cpu_context.last_sample_time.tv_nsec) / 1000000000.0;
        long tick_diff = current_ticks - g_cpu_context.last_process_ticks;
        // 核心当量 = 消耗秒数 / 墙上时间
        usage_cores = ((double)tick_diff / g_cpu_context.clock_ticks_per_sec) / time_diff;
    }

    // 更新采样缓存
    g_cpu_context.last_sample_time = now;
    g_cpu_context.last_process_ticks = current_ticks;

    // --- 第二部分：实测单核物理极限 DMIPS ---
    struct timespec start, end;
    volatile int a = 0, b = 2, c = 3;
    clock_gettime(CLOCK_MONOTONIC, &start);
    for (long i = 0; i < iterations; i++) {
        a = b + c; b = a - c; c = a * b;
        a = c / (b | 1); if (a > 100) a = 0;
    }
    clock_gettime(CLOCK_MONOTONIC, &end);
    double bench_elapsed = (end.tv_sec - start.tv_sec) + (end.tv_nsec - start.tv_nsec) / 1000000000.0;
    double single_core_dmips = (bench_elapsed <= 0) ? 0.0 : (iterations / bench_elapsed) / 1757.0;

    // --- 第三部分：组装返回结果 ---
    jdoubleArray result = env->NewDoubleArray(2);
    jdouble out[2] = {usage_cores, single_core_dmips};
    env->SetDoubleArrayRegion(result, 0, 2, out);

    return result;
}

}
