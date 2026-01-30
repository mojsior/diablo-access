#pragma once

#include <string_view>

namespace devilution {

/**
 * Check if TalkBack or any accessibility service is enabled on Android
 *
 * This function is used to determine whether to enable the game's built-in
 * accessibility features. When TalkBack is active, we rely on the system's
 * screen reader. When it's inactive, we use the game's TTS system.
 *
 * @return true if TalkBack or accessibility is active, false otherwise
 */
bool IsAndroidAccessibilityEnabled();

/**
 * Speak text using Android's native TTS engine
 *
 * This function forwards speech requests to the AndroidTextToSpeech Kotlin class,
 * which uses Android's built-in TextToSpeech engine.
 *
 * @param text The text to speak
 * @param force If true, interrupt current speech and speak immediately
 */
void SpeakAndroidText(std::string_view text, bool force);

/**
 * Stop any current speech
 *
 * Stops the TTS engine and clears the speech queue.
 */
void StopAndroidSpeech();

/**
 * Check if Android TTS is ready and initialized
 *
 * @return true if TTS is ready to speak, false otherwise
 */
bool IsAndroidTTSReady();

/**
 * Handle touch gesture from Android
 *
 * This function processes touch events and converts them to game actions.
 * It's called by the GestureDetector Kotlin class.
 *
 * @param action Motion event action (ACTION_DOWN, ACTION_UP, etc.)
 * @param x X coordinate of touch event
 * @param y Y coordinate of touch event
 * @param time Timestamp of touch event in milliseconds
 * @return Gesture type code (0 = none, 1 = swipe left, 2 = swipe right, 3 = double tap)
 */
int HandleAndroidGesture(int action, float x, float y, long time);

// Gesture type constants
constexpr int GESTURE_NONE = 0;
constexpr int GESTURE_SWIPE_LEFT = 1;
constexpr int GESTURE_SWIPE_RIGHT = 2;
constexpr int GESTURE_DOUBLE_TAP = 3;

} // namespace devilution
