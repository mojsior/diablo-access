package org.diasurgical.devilutionx

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * AndroidTextToSpeech - Wrapper for Android's native Text-to-Speech engine
 *
 * This class provides a clean interface for the game to use Android's built-in
 * TTS engine. It handles initialization, language selection (with pt-BR priority),
 * speech queuing, and proper resource cleanup.
 *
 * Key features:
 * - Uses Android's native TTS (no external libraries required)
 * - Prioritizes pt-BR (Portuguese Brazil) if available
 * - Falls back to device default language
 * - Thread-safe speech operations
 * - Proper utterance management to prevent speech queue buildup
 *
 * @author Diablo Access Team
 * @version 1.0.0
 */
class AndroidTextToSpeech(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AndroidTextToSpeech"
        private var instance: AndroidTextToSpeech? = null

        /**
         * Initialize the TTS singleton
         * Called from DevilutionXSDLActivity.onCreate()
         */
        fun initialize(context: Context): Boolean {
            if (instance == null) {
                instance = AndroidTextToSpeech(context.applicationContext)
                return true
            }
            return false
        }

        /**
         * Get the singleton instance
         */
        fun getInstance(): AndroidTextToSpeech {
            return instance ?: throw IllegalStateException(
                "AndroidTextToSpeech not initialized. Call initialize() first."
            )
        }

        /**
         * Shutdown the TTS engine
         * Called from DevilutionXSDLActivity.onDestroy()
         */
        fun shutdown() {
            instance?.shutdown()
            instance = null
        }

        /**
         * Speak text through TTS
         * JNI method called from C++ code
         *
         * @param text The text to speak
         * @param force If true, interrupt current speech and speak immediately
         */
        @JvmStatic
        external fun speak(text: String, force: Boolean)

        /**
         * Stop current speech
         * JNI method called from C++ code
         */
        @JvmStatic
        external fun stop()

        /**
         * Check if TTS is ready
         * JNI method called from C++ code
         */
        @JvmStatic
        external fun isReady(): Boolean
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastSpokenText = ""
    private val speechQueueLock = Any()

    /**
     * Initialization listener callback
     * Called when TTS engine initialization completes
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsInstance = tts ?: return

            // Try to set language to Portuguese Brazil (pt-BR) first
            // If that fails, try Portuguese (pt), then device default
            val ptBR = Locale("pt", "BR")
            val pt = Locale("pt")
            val default = Locale.getDefault()

            var langResult = TextToSpeech.LANG_MISSING_DATA
            var countryResult = TextToSpeech.LANG_MISSING_DATA

            // Try pt-BR first (priority language)
            val ptBRAvailable = try {
                val result = ttsInstance.setLanguage(ptBR)
                result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
            } catch (e: Exception) {
                Log.w(TAG, "pt-BR not available: ${e.message}")
                false
            }

            when {
                ptBRAvailable -> {
                    Log.i(TAG, "TTS language set to pt-BR (Portuguese Brazil)")
                }
                // Fall back to generic Portuguese
                ttsInstance.isLanguageAvailable(pt) >= TextToSpeech.LANG_AVAILABLE -> {
                    ttsInstance.setLanguage(pt)
                    Log.i(TAG, "TTS language set to pt (Portuguese)")
                }
                // Fall back to device default
                else -> {
                    ttsInstance.setLanguage(default)
                    Log.i(TAG, "TTS language set to device default: $default")
                }
            }

            // Set speech rate (1.0 = normal)
            ttsInstance.setSpeechRate(1.0f)

            // Set pitch (1.0 = normal)
            ttsInstance.setPitch(1.0f)

            // Add utterance progress listener for better control
            ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Speech completed: $utteranceId")
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "Speech error: $utteranceId")
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    Log.d(TAG, "Speech stopped: $utteranceId, interrupted: $interrupted")
                }
            })

            isInitialized = true
            Log.i(TAG, "Text-to-Speech initialized successfully")
        } else {
            Log.e(TAG, "Failed to initialize Text-to-Speech (status: $status)")
            isInitialized = false
        }
    }

    /**
     * Speak text through the TTS engine
     *
     * @param text The text to speak
     * @param force If true, interrupt current speech and speak immediately
     */
    fun speakText(text: String, force: Boolean) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak: $text")
            return
        }

        val ttsInstance = tts ?: return

        synchronized(speechQueueLock) {
            // Check if this is the same as the last spoken text
            // If not forced, skip speaking duplicate text
            if (!force && text == lastSpokenText) {
                Log.d(TAG, "Skipping duplicate text: $text")
                return
            }

            lastSpokenText = text

            // Clean the text - remove any null characters or invalid sequences
            val cleanText = text.replace("\u0000", "").trim()

            if (cleanText.isEmpty()) {
                Log.w(TAG, "Empty text after cleaning, skipping")
                return
            }

            // Set unique utterance ID for tracking
            val utteranceId = "utterance_${System.currentTimeMillis()}"

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            // Speak the text
            // If force is true, interrupt current speech (QUEUE_FLUSH)
            // Otherwise, add to queue (QUEUE_ADD)
            val queueMode = if (force) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            val result = ttsInstance.speak(cleanText, queueMode, params, utteranceId)

            if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "Speaking: $cleanText (force=$force)")
            } else {
                Log.e(TAG, "Failed to speak text (error code: $result): $cleanText")
            }
        }
    }

    /**
     * Stop any current speech
     */
    fun stopSpeech() {
        if (!isInitialized) {
            return
        }

        tts?.stop()
        Log.d(TAG, "Speech stopped")
    }

    /**
     * Shutdown the TTS engine and release resources
     */
    private fun shutdown() {
        synchronized(speechQueueLock) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.i(TAG, "Text-to-Speech shut down")
        }
    }

    /**
     * Check if TTS is initialized and ready
     */
    fun isTtsReady(): Boolean {
        return isInitialized && tts != null
    }

    /**
     * Get the current language being used by TTS
     */
    fun getCurrentLanguage(): Locale? {
        return if (isInitialized) {
            tts?.language
        } else {
            null
        }
    }

    /**
     * Get available languages (for debugging/settings)
     */
    fun getAvailableLanguages(): List<Locale> {
        return if (isInitialized) {
            // Note: getAvailableLanguages() is API level 21+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tts?.availableLanguages?.toList() ?: emptyList()
            } else {
                // For older versions, return empty list
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Set speech rate (0.5 to 2.0, where 1.0 is normal)
     */
    fun setSpeechRate(rate: Float) {
        if (isInitialized) {
            val clampedRate = rate.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(clampedRate)
            Log.d(TAG, "Speech rate set to: $clampedRate")
        }
    }

    /**
     * Set pitch (0.5 to 2.0, where 1.0 is normal)
     */
    fun setPitch(pitch: Float) {
        if (isInitialized) {
            val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
            tts?.setPitch(clampedPitch)
            Log.d(TAG, "Pitch set to: $clampedPitch")
        }
    }

    init {
        // Initialize TTS engine
        // This will call onInit() when initialization completes
        tts = TextToSpeech(context, this)
        Log.i(TAG, "Initializing Text-to-Speech...")
    }
}
