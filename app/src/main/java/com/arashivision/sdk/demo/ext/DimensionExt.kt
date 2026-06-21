package com.arashivision.sdk.demo.ext

import android.content.Context
import android.graphics.Point
import android.util.Size
import android.util.TypedValue
import android.view.WindowManager
import com.arashivision.sdk.demo.InstaApp

val screenSize: () -> Size = {
    val point = Point()
    val windowManager = InstaApp.instance.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getSize(point)
    Size(point.x, point.y)
}

val screenWidth: Int = screenSize().width

val screenHeight: Int = screenSize().height

val Float.px
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        InstaApp.instance.resources.displayMetrics
    )

val Float.dp
    get() = dp2px(this)

fun dp2px(dp: Float): Int {
    //SHARP AQUOS sense4 lite SH-RM15 出现转换完结果为0，防一下异常情况
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, InstaApp.instance.resources.displayMetrics)
    return (if (px > 0) px else dp).toInt()
}

fun px2dp(px: Float): Int {
    //SHARP AQUOS sense4 lite SH-RM15 出现转换完结果为0，防一下异常情况
    val scale =  InstaApp.instance.resources.displayMetrics.density
    return (if (scale > 0) (px / scale + 0.5f) else px).toInt()
}

fun sp2px(sp: Float): Int {
    //SHARP AQUOS sense4 lite SH-RM15 出现转换完结果为0，防一下异常情况
    val px = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        InstaApp.instance.resources.displayMetrics
    )
    return (if (px > 0) px else sp).toInt()
}