package org.diasurgical.devilutionx

import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * AccessibilityManager - Detects screen reader status on Android
 *
 * This class provides functionality to check if TalkBack or any other
 * accessibility service is active on the device. This is used to determine
 * whether to enable the game's built-in accessibility features.
 *
 * Key behavior:
 * - When TalkBack is ON: Use system screen reader (disable game TTS)
 * - When TalkBack is OFF: Enable game's built-in accessibility features
 *
 * @author Diablo Access Team
 * @version 1.0.0
 */
class AccessibilityManager(private val context: Context) {

    private val accessibilityManager: AccessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    /**
     * Check if any accessibility service (including TalkBack) is enabled
     *
     * @return true if TalkBack or any accessibility service is active
     */
    fun isTouchExplorationEnabled(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Check if TalkBack or any screen reader is active
     *
     * This is the main method called by the native C++ code to determine
     * whether the game should use its built-in TTS or rely on the system's
     * screen reader.
     *
     * @return true if a screen reader is active, false otherwise
     */
    fun isScreenReaderActive(): Boolean {
        // Check if touch exploration (TalkBack's navigation mode) is enabled
        val touchExploration = accessibilityManager.isTouchExplorationEnabled

        // Additional check: see if any accessibility service is enabled
        // This covers cases where touch exploration might be disabled but
        // other accessibility services are running
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityManager.FEEDBACK_SPOKEN
        )

        val hasSpokenFeedback = !enabledServices.isEmpty()

        // Return true if either touch exploration or spoken feedback is enabled
        return touchExploration || hasSpokenFeedback
    }

    /**
     * Get the list of enabled accessibility service names (for debugging)
     *
     * @return Comma-separated list of accessibility service names
     */
    fun getEnabledAccessibilityServiceNames(): String {
        val services = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityManager.FEEDBACK_SPOKEN
        )

        if (services.isEmpty()) {
            return "None"
        }

        return services.joinToString(", ") { service ->
            service.resolveInfo.serviceInfo?.loadLabel(context.packageManager)?.toString()
                ?: "Unknown Service"
        }
    }

    companion object {
        /**
         * Singleton instance for JNI access
         */
        private var instance: AccessibilityManager? = null

        /**
         * Initialize the AccessibilityManager singleton
         * Called from DevilutionXSDLActivity.onCreate()
         */
        fun initialize(context: Context) {
            if (instance == null) {
                instance = AccessibilityManager(context.applicationContext)
            }
        }

        /**
         * Get the singleton instance
         */
        fun getInstance(): AccessibilityManager {
            return instance ?: throw IllegalStateException(
                "AccessibilityManager not initialized. Call initialize() first."
            )
        }

        /**
         * JNI method - Check if screen reader is active
         * This is called from C++ code
         */
        @JvmStatic
        external fun isScreenReaderEnabled(): Boolean

        /**
         * JNI method - Get enabled accessibility service names
         * This is called from C++ code for debugging
         */
        @JvmStatic
        external fun getAccessibilityServices(): String
    }
}
