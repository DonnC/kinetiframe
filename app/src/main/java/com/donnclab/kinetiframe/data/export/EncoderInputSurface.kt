package com.donnclab.kinetiframe.data.export

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reused bitmap texture on a recordable EGL surface; input timestamps belong to the timeline. */
internal class EncoderInputSurface(surface: Surface, private val width: Int, private val height: Int) {
    private val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val versions = IntArray(2)
    private val config = arrayOfNulls<android.opengl.EGLConfig>(1)
    private val context: android.opengl.EGLContext
    private val window: android.opengl.EGLSurface
    private var program = 0
    private var texture = 0
    private var allocated = false
    private val vertices = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f))
        position(0)
    }

    init {
        check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "Could not initialize export EGL" }
        check(
            EGL14.eglChooseConfig(
                display,
                intArrayOf(
                    EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT, 0x3142, 1, EGL14.EGL_NONE,
                ),
                0,
                config,
                0,
                1,
                IntArray(1),
                0,
            ) && config[0] != null,
        ) { "No recordable export surface" }
        context = EGL14.eglCreateContext(
            display,
            config[0],
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0,
        )
        window = EGL14.eglCreateWindowSurface(display, config[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(context != EGL14.EGL_NO_CONTEXT && window != EGL14.EGL_NO_SURFACE) { "Could not create export EGL" }
        current()
        try {
            val vertex =
                shader(
                    GLES20.GL_VERTEX_SHADER,
                    "attribute vec2 p; attribute vec2 t; varying vec2 uv; void main(){gl_Position=vec4(p,0.0,1.0);uv=t;}",
                )
            val fragment =
                shader(
                    GLES20.GL_FRAGMENT_SHADER,
                    "precision mediump float; varying vec2 uv; uniform sampler2D image; void main(){gl_FragColor=texture2D(image,uv);}",
                )
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertex)
            GLES20.glAttachShader(program, fragment)
            GLES20.glLinkProgram(program)
            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] != 0) { GLES20.glGetProgramInfoLog(program) }
            GLES20.glDeleteShader(vertex)
            GLES20.glDeleteShader(fragment)
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            texture = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        } finally {
            detach()
        }
    }

    fun draw(bitmap: Bitmap, timeUs: Long) {
        // Coroutine resumption may change worker threads. Detach after each submission instead
        // of leaving a context owned by a thread while the compositor suspends for disk reads.
        current()
        try {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            if (allocated) {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
            } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                allocated = true
            }
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "image"), 0)
            val position = GLES20.glGetAttribLocation(program, "p")
            val texcoord = GLES20.glGetAttribLocation(program, "t")
            vertices.position(0)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, vertices)
            vertices.position(2)
            GLES20.glVertexAttribPointer(texcoord, 2, GLES20.GL_FLOAT, false, 16, vertices)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(texcoord)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Export texture rendering failed" }
            check(EGLExt.eglPresentationTimeANDROID(display, window, Math.multiplyExact(timeUs, 1000L)))
            check(EGL14.eglSwapBuffers(display, window)) { "Could not submit export frame" }
        } finally {
            detach()
        }
    }

    fun release() {
        current()
        try {
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            GLES20.glDeleteProgram(program)
        } finally {
            detach()
            EGL14.eglDestroySurface(display, window)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
        }
    }

    private fun current() {
        check(EGL14.eglMakeCurrent(display, window, window, context))
    }
    private fun detach() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    }
    private fun shader(kind: Int, source: String): Int {
        val id = GLES20.glCreateShader(kind)
        GLES20.glShaderSource(id, source)
        GLES20.glCompileShader(id)
        val status = IntArray(1)
        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] != 0) { GLES20.glGetShaderInfoLog(id) }
        return id
    }
}
