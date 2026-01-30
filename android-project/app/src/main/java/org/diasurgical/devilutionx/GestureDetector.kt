package org.diasurgical.devilutionx

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * GestureDetector - Custom gesture detection for accessibility navigation
 *
 * This class provides custom gesture handling for the game's accessibility features.
 * It detects simple swipe gestures and double-tap actions to navigate game elements
 * without requiring visual feedback.
 *
 * Supported gestures:
 * - Swipe Left: Navigate to previous item/target
 * - Swipe Right: Navigate to next item/target
 * - Double Tap: Select/activate current item/target
 *
 * Integration:
 * - This detector should be attached to the game's SDL surface view
 * - Gestures are translated to native key events that the game understands
 * - Works in conjunction with the game's accessibility system
 *
 * @author Diablo Access Team
 * @version 1.0.0
 */
class GestureDetector(context: Context) : View.OnTouchListener {

    companion object {
        private const val TAG = "GestureDetector"

        // Gesture thresholds
        private const val SWIPE_MIN_DISTANCE = 100
        private const val SWIPE_MAX_VELOCITY = 1000
        private const val SWIPE_THRESHOLD_VELOCITY = 100

        // Double tap detection
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val DOUBLE_TAP_DISTANCE = 100

        // Singleton instance for JNI access
        private var instance: GestureDetector? = null

        /**
         * Initialize the gesture detector singleton
         * Called from DevilutionXSDLActivity.onCreate()
         */
        fun initialize(context: Context) {
            if (instance == null) {
                instance = GestureDetector(context.applicationContext)
            }
        }

        /**
         * Get the singleton instance
         */
        fun getInstance(): GestureDetector {
            return instance ?: throw IllegalStateException(
                "GestureDetector not initialized. Call initialize() first."
            )
        }

        /**
         * JNI method - Handle gesture event
         * Called from C++ when SDL receives touch events
         *
         * @param action The motion event action (DOWN, UP, MOVE, etc.)
         * @param x X coordinate
         * @param y Y coordinate
         * @param time Time of event in milliseconds
         * @return Gesture type code (0 = none, 1 = swipe left, 2 = swipe right, 3 = double tap)
         */
        @JvmStatic
        external fun handleGesture(action: Int, x: Float, y: Float, time: Long): Int

        /**
         * Gesture type codes (returned to native code)
         */
        const val GESTURE_NONE = 0
        const val GESTURE_SWIPE_LEFT = 1
        const val GESTURE_SWIPE_RIGHT = 2
        const val GESTURE_DOUBLE_TAP = 3
    }

    // Android GestureDetector for low-level gesture detection
    private val gestureDetector: android.view.GestureDetector

    // Track first tap for double-tap detection
    private var firstTapX = 0f
    private var firstTapY = 0f
    private var firstTapTime = 0L
    private var isWaitingForSecondTap = false

    // Gesture listener callbacks (can be set by game code)
    private var onSwipeLeftListener: (() -> Unit)? = null
    private var onSwipeRightListener: (() -> Unit)? = null
    private var onDoubleTapListener: (() -> Unit)? = null

    init {
        gestureDetector = android.view.GestureDetector(
            context,
            object : android.view.GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    // Always return true to receive all events
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // Single tap detected (not part of double tap)
                    Log.d(TAG, "Single tap at (${e.x}, ${e.y})")
                    return false
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Double tap detected
                    Log.d(TAG, "Double tap at (${e.x}, ${e.y})")
                    onDoubleTapListener?.invoke()
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    // Handle double tap events
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    // Scroll gesture - not used for accessibility
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    // Detect swipe gestures
                    if (e1 == null || e2 == null) return false

                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y

                    // Check if horizontal swipe (more horizontal than vertical movement)
                    if (abs(deltaX) > abs(deltaY)) {
                        if (abs(deltaX) > SWIPE_MIN_DISTANCE &&
                            abs(velocityX) > SWIPE_THRESHOLD_VELOCITY &&
                            abs(velocityX) < SWIPE_MAX_VELOCITY
                        ) {
                            if (deltaX > 0) {
                                // Swipe right (previous)
                                Log.d(TAG, "Swipe right detected")
                                onSwipeRightListener?.invoke()
                            } else {
                                // Swipe left (next)
                                Log.d(TAG, "Swipe left detected")
                                onSwipeLeftListener?.invoke()
                            }
                            return true
                        }
                    }

                    return false
                }
            }
        )
    }

    /**
     * Handle touch events from the SDL surface
     * This is called by SDL's touch event handling
     */
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        return event?.let {
            gestureDetector.onTouchEvent(it)
        } ?: false
    }

    /**
     * Process raw touch event data (for JNI integration)
     * This allows C++ code to pass touch events directly
     *
     * @param action Motion event action (ACTION_DOWN, ACTION_UP, etc.)
     * @param x X coordinate
     * @param y Y coordinate
     * @param time Event timestamp
     * @return Gesture type code
     */
    fun processTouchEvent(action: Int, x: Float, y: Float, time: Long): Int {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                return handleActionDown(x, y, time)
            }
            MotionEvent.ACTION_UP -> {
                return handleActionUp(x, y, time)
            }
            MotionEvent.ACTION_MOVE -> {
                // Could be used for fling/swipe detection
                return GESTURE_NONE
            }
        }
        return GESTURE_NONE
    }

    /**
     * Handle touch down event
     */
    private fun handleActionDown(x: Float, y: Float, time: Long): Int {
        if (isWaitingForSecondTap) {
            // Check if this tap is close enough to the first tap
            val distance = Math.sqrt(
                Math.pow((x - firstTapX).toDouble(), 2.0) +
                    Math.pow((y - firstTapY).toDouble(), 2.0)
            )

            val timeDelta = time - firstTapTime

            if (distance < DOUBLE_TAP_DISTANCE && timeDelta < DOUBLE_TAP_TIMEOUT) {
                // Double tap detected
                isWaitingForSecondTap = false
                Log.d(TAG, "Double tap detected at ($x, $y)")
                onDoubleTapListener?.invoke()
                return GESTURE_DOUBLE_TAP
            } else {
                // Too far or too slow, start new tap sequence
                isWaitingForSecondTap = false
            }
        }

        // Record first tap
        firstTapX = x
        firstTapY = y
        firstTapTime = time
        isWaitingForSecondTap = true

        return GESTURE_NONE
    }

    /**
     * Handle touch up event
     */
    private fun handleActionUp(x: Float, y: Float, time: Long): Int {
        // Swipe detection is handled in ACTION_DOWN/ACTION_MOVE
        // This is a simplified version - full implementation would track
        // movement history

        if (!isWaitingForSecondTap) {
            return GESTURE_NONE
        }

        // Check for swipe (simple horizontal movement detection)
        val deltaX = x - firstTapX
        val deltaY = y - firstTapY
        val timeDelta = time - firstTapTime

        if (abs(deltaX) > abs(deltaY) && abs(deltaX) > SWIPE_MIN_DISTANCE) {
            // Horizontal swipe
            val velocity = abs(deltaX) / timeDelta * 1000

            if (velocity > SWIPE_THRESHOLD_VELOCITY) {
                isWaitingForSecondTap = false

                if (deltaX > 0) {
                    Log.d(TAG, "Swipe right detected")
                    onSwipeRightListener?.invoke()
                    return GESTURE_SWIPE_RIGHT
                } else {
                    Log.d(TAG, "Swipe left detected")
                    onSwipeLeftListener?.invoke()
                    return GESTURE_SWIPE_LEFT
                }
            }
        }

        // Not a swipe, still waiting for potential second tap
        return GESTURE_NONE
    }

    /**
     * Set listener for swipe left gestures
     */
    fun setOnSwipeLeftListener(listener: () -> Unit) {
        onSwipeLeftListener = listener
    }

    /**
     * Set listener for swipe right gestures
     */
    fun setOnSwipeRightListener(listener: () -> Unit) {
        onSwipeRightListener = listener
    }

    /**
     * Set listener for double tap gestures
     */
    fun setOnDoubleTapListener(listener: () -> Unit) {
        onDoubleTapListener = listener
    }

    /**
     * Reset gesture state (call when game pauses/exits)
     */
    fun reset() {
        isWaitingForSecondTap = false
        firstTapX = 0f
        firstTapY = 0f
        firstTapTime = 0L
    }

    /**
     * Configure gesture thresholds (for accessibility settings)
     */
    fun configureThresholds(
        swipeMinDistance: Int = SWIPE_MIN_DISTANCE,
        swipeThresholdVelocity: Int = SWIPE_THRESHOLD_VELOCITY,
        doubleTapTimeout: Long = DOUBLE_TAP_TIMEOUT,
        doubleTapDistance: Int = DOUBLE_TAP_DISTANCE
    ) {
        // Note: In Kotlin, companion object constants can't be modified
        // This would require refactoring to use instance variables
        Log.d(TAG, "Gesture thresholds configured (not implemented in this version)")
    }
}
