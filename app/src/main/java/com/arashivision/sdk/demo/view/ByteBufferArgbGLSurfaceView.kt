package com.arashivision.sdk.demo.view

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 使用OpenGL ES处理ByteBuffer类型ARGB数据的高效实现
 */
class ByteBufferArgbGLSurfaceView : GLSurfaceView, GLSurfaceView.Renderer {
    // 顶点坐标
    private val vertexCoords = floatArrayOf(
        -1.0f, 1.0f, 0.0f,  // 左上
        -1.0f, -1.0f, 0.0f,  // 左下
        1.0f, -1.0f, 0.0f,  // 右下
        1.0f, 1.0f, 0.0f // 右上
    )

    // 纹理坐标
    private val texCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f
    )

    // 绘制索引
    private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    private var program = 0
    private var textureId = 0
    private var aPositionLoc = 0
    private var aTexCoordLoc = 0
    private var mMvpMatrixLoc = 0
    private var sTextureLoc = 0
    private val mvpMatrix = FloatArray(16)
    private var vertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var width = 0
    private var height = 0
    private var argbBuffer: ByteBuffer? = null
    private var dataUpdated = false
    private val lock = Any()

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        setEGLContextClientVersion(2)
        setRenderer(this)
        setRenderMode(RENDERMODE_WHEN_DIRTY) // 按需渲染，节省资源

        // 初始化缓冲区
        initBuffers()
    }

    /**
     * 更新ARGB ByteBuffer数据
     * @param buffer 包含ARGB数据的ByteBuffer
     * @param w 宽度
     * @param h 高度
     */
    fun updateArgbBuffer(buffer: ByteBuffer?, w: Int, h: Int) {
        if (buffer == null || buffer.remaining() < w * h * 4) {
            return
        }

        synchronized(lock) {
            // 复制缓冲区数据（避免外部修改影响）
            this.argbBuffer = ByteBuffer.allocateDirect(w * h * 4)
                .order(ByteOrder.nativeOrder())
            buffer.mark()
            this.argbBuffer!!.put(buffer)
            buffer.reset()
            this.argbBuffer!!.rewind()

            this.width = w
            this.height = h
            dataUpdated = true
        }

        requestRender() // 请求渲染
    }

    private fun initBuffers() {
        // 顶点缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer!!.put(vertexCoords).position(0)

        // 纹理坐标缓冲区
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texCoordBuffer!!.put(texCoords).position(0)

        // 索引缓冲区
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        indexBuffer!!.put(indices).position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 创建着色器程序
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) {
            return
        }

        // 获取着色器变量位置
        aPositionLoc = GLES20.glGetAttribLocation(program, "vPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "vTexCoord")
        mMvpMatrixLoc = GLES20.glGetUniformLocation(program, "mvpMatrix")
        sTextureLoc = GLES20.glGetUniformLocation(program, "sTexture")

        // 初始化纹理
        initTexture()

        // 设置MVP矩阵
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清除屏幕
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 使用着色器程序
        GLES20.glUseProgram(program)

        // 更新纹理数据
        synchronized(lock) {
            if (dataUpdated && argbBuffer != null && width > 0 && height > 0) {
                updateTexture()
                dataUpdated = false
            }
        }

        // 启用顶点属性
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)

        // 设置顶点坐标
        GLES20.glVertexAttribPointer(
            aPositionLoc, 3, GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )

        // 设置纹理坐标
        GLES20.glVertexAttribPointer(
            aTexCoordLoc, 2, GLES20.GL_FLOAT, false,
            8, texCoordBuffer
        )

        // 设置MVP矩阵
        GLES20.glUniformMatrix4fv(mMvpMatrixLoc, 1, false, mvpMatrix, 0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(sTextureLoc, 0)

        // 绘制
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, indices.size,
            GLES20.GL_UNSIGNED_SHORT, indexBuffer
        )

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
    }

    private fun initTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 设置纹理参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun updateTexture() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 注意：ARGB格式需要转换为OpenGL的RGBA格式
        // 如果数据是ARGB，这里的内部格式和格式参数需要调整
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            argbBuffer
        )
    }

    // 着色器程序创建工具方法
    private fun createProgram(vertexSource: String?, fragmentSource: String?): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String?): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    companion object {
        private val VERTEX_SHADER = "attribute vec4 vPosition;\n" +
                "attribute vec2 vTexCoord;\n" +
                "uniform mat4 mvpMatrix;\n" +
                "varying vec2 texCoord;\n" +
                "void main() {\n" +
                "  gl_Position = mvpMatrix * vPosition;\n" +
                "  texCoord = vTexCoord;\n" +
                "}\n"

        private val FRAGMENT_SHADER = "precision mediump float;\n" +
                "varying vec2 texCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "void main() {\n" +
                "  vec4 color = texture2D(sTexture, texCoord);\n" +
                "  gl_FragColor = color;\n" +
                "}\n"
    }
}

