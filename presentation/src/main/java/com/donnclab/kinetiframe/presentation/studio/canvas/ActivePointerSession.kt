package com.donnclab.kinetiframe.presentation.studio.canvas

/**
 * Keeps one drawing interaction attached to the pointer that started it.
 *
 * Pointer indices are positional and may change as fingers or a resting palm are
 * added/removed. Pointer IDs are stable for the lifetime of a contact, so this
 * tiny state holder is deliberately independent of [android.view.MotionEvent]
 * and can be unit tested without an Android device.
 */
internal class ActivePointerSession(private val noPointerId: Int = NO_POINTER_ID) {
    var pointerId: Int = noPointerId
        private set

    var startX: Float = 0f
    var startY: Float = 0f

    var startTime: Long = 0L
        private set

    val hasActivePointer: Boolean
        get() = pointerId != noPointerId

    fun begin(pointerId: Int, eventTime: Long = 0L) {
        this.pointerId = pointerId
        startTime = eventTime
    }

    fun isActive(pointerId: Int): Boolean = this.pointerId == pointerId

    fun shouldIgnorePointerUp(pointerId: Int): Boolean = hasActivePointer && !isActive(pointerId)

    /** Returns true only when the pointer ending the session is the drawing pointer. */
    fun end(pointerId: Int): Boolean {
        if (!isActive(pointerId)) return false
        clear()
        return true
    }

    fun clear() {
        pointerId = noPointerId
        startTime = 0L
    }

    private companion object {
        const val NO_POINTER_ID = -1
    }
}

/** Touch navigation owns two-finger gestures; a stylus plus a resting palm does not. */
internal object ViewportGesturePolicy {
    fun shouldTransform(pressedPointerCount: Int, hasStylusContact: Boolean): Boolean =
        pressedPointerCount >= 2 && !hasStylusContact
}
