package com.arashivision.sdk.demo.ui.play

import androidx.lifecycle.viewModelScope
import com.arashivision.sdk.demo.base.BaseViewModel
import com.arashivision.sdk.demo.base.EventStatus
import com.arashivision.sdk.demo.util.SPUtils
import com.arashivision.sdk.demo.util.StorageUtils
import com.arashivision.sdkmedia.export.ExportImageParamsBuilder
import com.arashivision.sdkmedia.export.ExportUtils
import com.arashivision.sdkmedia.export.ExportUtils.ExportMode
import com.arashivision.sdkmedia.export.ExportVideoParamsBuilder
import com.arashivision.sdkmedia.export.IExportCallback
import com.arashivision.sdkmedia.params.PlayerParamsBuilder
import com.arashivision.sdkmedia.player.config.InstaStabType
import com.arashivision.sdkmedia.player.image.ImageParamsBuilder
import com.arashivision.sdkmedia.player.image.InstaImagePlayerView
import com.arashivision.sdkmedia.player.offset.OffsetType
import com.arashivision.sdkmedia.player.video.InstaVideoPlayerView
import com.arashivision.sdkmedia.player.video.VideoParamsBuilder
import com.arashivision.sdkmedia.stitch.StitchUtils
import com.arashivision.sdkmedia.work.WorkWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays

class WorkPlayViewModel : BaseViewModel(), IExportCallback {

    private var mPlayerParamsBuilder: PlayerParamsBuilder<*>? = null

    private var gestureEnabled: Boolean
        get() = mPlayerParamsBuilder?.isGestureEnabled ?: SPUtils.getBoolean("GestureEnabled", true)
        set(enable) {
            mPlayerParamsBuilder!!.isGestureEnabled = enable
            SPUtils.putBoolean("GestureEnabled", enable)
        }

    var isLrvEnable: Boolean
        get() {
            if (mPlayerParamsBuilder == null) return SPUtils.getBoolean("LrvEnable", false)
            if (mPlayerParamsBuilder is VideoParamsBuilder) {
                return (mPlayerParamsBuilder as VideoParamsBuilder).isLrvEnable
            }
            return false
        }
        set(enable) {
            if (mPlayerParamsBuilder != null && mPlayerParamsBuilder is VideoParamsBuilder) {
                (mPlayerParamsBuilder as VideoParamsBuilder).isLrvEnable = enable
                SPUtils.putBoolean("LrvEnable", enable)
            }
        }

    var isColorPlusEnable: Boolean
        get() = mPlayerParamsBuilder?.isColorPlusEnable ?: SPUtils.getBoolean("ColorPlus", false)
        set(enable) {
            mPlayerParamsBuilder!!.isColorPlusEnable = enable
            SPUtils.putBoolean("ColorPlus", enable)
        }

    var isImageFusion: Boolean
        get() = mPlayerParamsBuilder?.isImageFusion ?: SPUtils.getBoolean("ImageFusion", false)
        set(enable) {
            mPlayerParamsBuilder!!.isImageFusion = enable
            SPUtils.putBoolean("ImageFusion", enable)
        }


    var isDePurpleFilterOn: Boolean
        get() = mPlayerParamsBuilder?.isDePurpleFilterOn ?: SPUtils.getBoolean("DePurpleFilter", false)
        set(enable) {
            mPlayerParamsBuilder!!.isDePurpleFilterOn = enable
            SPUtils.putBoolean("DePurpleFilter", enable)
        }

    var isDynamicStitch: Boolean
        get() = mPlayerParamsBuilder?.isDynamicStitch ?: SPUtils.getBoolean("DynamicStitch", false)
        set(enable) {
            mPlayerParamsBuilder!!.isDynamicStitch = enable
            SPUtils.putBoolean("DynamicStitch", enable)
        }

    private var withSwitchingAnimation: Boolean
        get() = mPlayerParamsBuilder?.isWithSwitchingAnimation ?: SPUtils.getBoolean("WithSwitchingAnimation", true)
        set(enable) {
            mPlayerParamsBuilder!!.isWithSwitchingAnimation = enable
            SPUtils.putBoolean("WithSwitchingAnimation", enable)
        }

    var offsetType: OffsetType
        get() = mPlayerParamsBuilder?.offsetType ?: run {
            val type: String = SPUtils.getString("OffsetType", "ORIGINAL")
            for (value in OffsetType.entries) {
                if (value.name == type) {
                    return value
                }
            }
            return OffsetType.ORIGINAL
        }
        set(offsetType) {
            mPlayerParamsBuilder!!.offsetType = offsetType
            SPUtils.putString("OffsetType", offsetType.name)
        }

    val screenRatio: IntArray
        get() = mPlayerParamsBuilder?.screenRatio ?: run {
            val value: String = SPUtils.getString("ScreenRatio", "9:16")
            Arrays.stream(value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).mapToInt { s: String -> s.toInt() }.toArray()
        }

    fun setScreenRatio(ratioX: Int, ratioY: Int) {
        mPlayerParamsBuilder!!.setScreenRatio(ratioX, ratioY)
        SPUtils.putString("ScreenRatio", "$ratioX:$ratioY")
    }

    var stabType: Int
        get() = mPlayerParamsBuilder?.stabType ?: SPUtils.getInt("StabType", InstaStabType.STAB_TYPE_OFF)
        set(type) {
            mPlayerParamsBuilder!!.stabType = type
            SPUtils.putInt("StabType", type)
        }

    var renderModelType: Int
        get() = mPlayerParamsBuilder?.renderModelType ?: SPUtils.getInt("RenderModelType", PlayerParamsBuilder.RENDER_MODE_AUTO)
        set(type) {
            mPlayerParamsBuilder!!.renderModelType = type
            SPUtils.putInt("RenderModelType", type)
        }

    private var exportId: Int = -1
    private var exportPath: String = ""

    var hdrStitchPath = ""
        private set
    var pureShotStitchPath = ""
        private set

    var showHdr: Boolean = false
        private set
    var showPureShot: Boolean = false
        private set

    private fun createExportImageParamsBuilder(player: InstaImagePlayerView, exportWidth: Int, exportHeight: Int): ExportImageParamsBuilder {
        val imageBuilder = ExportImageParamsBuilder()
        imageBuilder.targetPath = StorageUtils.exportImageDir + "/" + System.currentTimeMillis() + ".jpg"
        imageBuilder.stabType = player.stabType
        imageBuilder.offsetType = player.offsetType
        imageBuilder.isImageFusion = player.isImageFusion
        if (renderModelType == PlayerParamsBuilder.RENDER_MODE_PLANE_STITCH) {
            imageBuilder.exportMode = ExportMode.PANORAMA
            imageBuilder.setScreenRatio(2, 1)
        } else {
            imageBuilder.exportMode = ExportMode.SPHERE
            imageBuilder.setScreenRatio(player.screenRatio[0], player.screenRatio[1])
        }
        imageBuilder.width = exportWidth
        imageBuilder.height = exportHeight
        imageBuilder.isDePurpleFilterOn = isDePurpleFilterOn
        imageBuilder.isDynamicStitch = player.isDynamicStitch
        imageBuilder.isColorPlusEnable = player.isColorPlusEnable

        imageBuilder.distance = player.distance
        imageBuilder.yaw = player.yaw
        imageBuilder.fov = player.fov
        imageBuilder.pitch = player.pitch
        return imageBuilder
    }

    private fun createExportVideoParamsBuilder(player: InstaVideoPlayerView, exportWidth: Int, exportHeight: Int, fps: Int, bitrate: Int, denoise: Boolean): ExportVideoParamsBuilder {
        val videoBuilder = ExportVideoParamsBuilder()
        videoBuilder.targetPath = StorageUtils.exportVideoDir + "/" + System.currentTimeMillis() + ".mp4"
        videoBuilder.stabType = player.stabType
        videoBuilder.offsetType = player.offsetType
        videoBuilder.isImageFusion = player.isImageFusion
        if (renderModelType == PlayerParamsBuilder.RENDER_MODE_PLANE_STITCH) {
            videoBuilder.exportMode = ExportMode.PANORAMA
            videoBuilder.setScreenRatio(2, 1)
        } else {
            videoBuilder.exportMode = ExportMode.SPHERE
            videoBuilder.setScreenRatio(player.screenRatio[0], player.screenRatio[1])
        }
        videoBuilder.width = exportWidth
        videoBuilder.height = exportHeight
        videoBuilder.fps = fps
        videoBuilder.isDenoise = denoise
        videoBuilder.bitrate = bitrate
        videoBuilder.isColorPlusEnable = player.isColorPlusEnable
        videoBuilder.isDePurpleFilterOn = player.isDePurpleFilterOn
        videoBuilder.isDynamicStitch = player.isDynamicStitch
        videoBuilder.fov = player.fov
        videoBuilder.distance = player.distance
        videoBuilder.yaw = player.yaw
        videoBuilder.roll = player.roll
        videoBuilder.pitch = player.pitch
        return videoBuilder
    }

    fun createPlayerParamsBuilder(workWrapper: WorkWrapper, showHdr: Boolean = false, showPureShot: Boolean = false, ): PlayerParamsBuilder<*> {
        if (mPlayerParamsBuilder != null) return mPlayerParamsBuilder as PlayerParamsBuilder<*>
        val builder = if (workWrapper.isVideo) {
            VideoParamsBuilder().also {
                it.isLrvEnable = isLrvEnable
            }
        } else {
            ImageParamsBuilder()
            // 缓存路径使用默认路径
            // builder.setCacheCutSceneRootPath("");
        }

        // 缓存路径使用默认路径
//        builder.setCacheCutSceneRootPath("");
//        builder.setCacheWorkThumbnailRootPath("");
//        builder.setStabilizerCacheRootPath("");
        builder.isGestureEnabled = gestureEnabled
        builder.isImageFusion = isImageFusion
        builder.isDePurpleFilterOn = isDePurpleFilterOn
        builder.isDynamicStitch = isDynamicStitch
        builder.isWithSwitchingAnimation = withSwitchingAnimation
        builder.offsetType = offsetType
        val screenRatio = screenRatio
        builder.setScreenRatio(screenRatio[0], screenRatio[1])
        builder.stabType = stabType
        builder.renderModelType = renderModelType
        this.showHdr = showHdr
        if (showHdr && hdrStitchPath.isNotEmpty()) {
            builder.urlForPlay = hdrStitchPath
        }
        this.showPureShot = showPureShot
        if (showPureShot && pureShotStitchPath.isNotEmpty()) {
            builder.urlForPlay = pureShotStitchPath
        }
        mPlayerParamsBuilder = builder
        return mPlayerParamsBuilder!!
    }


    fun exportImage(workWrapper: WorkWrapper, imagePlayerView: InstaImagePlayerView, width: Int, height: Int) {
        val createExportImageParamsBuilder = createExportImageParamsBuilder(imagePlayerView, width, height)
        exportPath = createExportImageParamsBuilder.targetPath
        ExportUtils.exportImage(workWrapper, createExportImageParamsBuilder, this)
    }

    fun exportVideo(workWrapper: WorkWrapper, player: InstaVideoPlayerView, exportWidth: Int, exportHeight: Int, fps: Int, bitrate: Int, denoise: Boolean) {
        val createExportVideoParamsBuilder = createExportVideoParamsBuilder(player, exportWidth, exportHeight,
            fps,
            if (bitrate == -1) workWrapper.bitrate else bitrate,
            denoise
        )
        exportPath = createExportVideoParamsBuilder.targetPath
        ExportUtils.exportVideo(workWrapper, createExportVideoParamsBuilder, this)
    }

    fun cancelExport() {
        ExportUtils.stopExport(exportId)
    }

    override fun onStart(id: Int) {
        exportId = id
        emitEvent(WorkPlayEvent.ExportMediaEvent(WorkPlayEvent.ExportStatus.START))
    }

    override fun onSuccess() {
        emitEvent(WorkPlayEvent.ExportMediaEvent(WorkPlayEvent.ExportStatus.SUCCESS, exportPath = exportPath))
    }

    override fun onFail(code: Int, msg: String?) {
        emitEvent(WorkPlayEvent.ExportMediaEvent(WorkPlayEvent.ExportStatus.FAILED, error = "$code:$msg"))
    }

    override fun onCancel() {
        emitEvent(WorkPlayEvent.ExportMediaEvent(WorkPlayEvent.ExportStatus.CANCEL))
    }


    fun hdrStitch(workWrapper: WorkWrapper) {
        emitEvent(WorkPlayEvent.HDRStitchEvent(EventStatus.START))
        viewModelScope.launch(Dispatchers.IO) {
            val output = StorageUtils.hdrStitchDir + "/hdr_" + System.currentTimeMillis() + ".jpg"
            val result = StitchUtils.generateHDR(workWrapper, output)
            withContext(Dispatchers.Main) {
                if (result) {
                    hdrStitchPath = output
                    emitEvent(WorkPlayEvent.HDRStitchEvent(EventStatus.SUCCESS))
                } else {
                    emitEvent(WorkPlayEvent.HDRStitchEvent(EventStatus.FAILED))
                }
            }
        }
    }

    fun pureShotStitch(workWrapper: WorkWrapper) {
        emitEvent(WorkPlayEvent.PureShotStitchEvent(EventStatus.START))
        viewModelScope.launch(Dispatchers.IO) {
            val output = StorageUtils.pureShotStitchDir + "/pure_shot_" + System.currentTimeMillis() + ".jpg"
            val result = StitchUtils.generatePureShot(workWrapper, output, "insta360/pure_shot_algo")
            withContext(Dispatchers.Main) {
                if (result == 0) {
                    pureShotStitchPath = output
                    emitEvent(WorkPlayEvent.PureShotStitchEvent(EventStatus.SUCCESS))
                } else {
                    emitEvent(WorkPlayEvent.PureShotStitchEvent(EventStatus.FAILED))
                }
            }
        }
    }

    override fun onProgress(progress: Float) {
        emitEvent(WorkPlayEvent.ExportMediaEvent(WorkPlayEvent.ExportStatus.PROGRESS,progress = progress))
    }

    fun tryLoadExtraData(workWrapper: WorkWrapper) {
        emitEvent(WorkPlayEvent.TryLoadExtraDataEvent(EventStatus.START))
        viewModelScope.launch(Dispatchers.IO) {
            if (!workWrapper.isExtraDataLoaded) {
                workWrapper.loadExtraData()
            }
            withContext(Dispatchers.Main) {
                emitEvent(WorkPlayEvent.TryLoadExtraDataEvent(EventStatus.SUCCESS))
            }
        }
    }
}
