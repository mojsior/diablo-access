#include "utils/screen_reader.hpp"

#include <jni.h>
#include <string>
#include <string_view>

namespace devilution {

// JNI method references (cached for performance)
static jmethodID g_isScreenReaderEnabled = nullptr;
static jmethodID g_speak = nullptr;
static jmethodID g_stop = nullptr;
static jmethodID g_isReady = nullptr;
static jmethodID g_handleGesture = nullptr;

// Global reference to JVM (needed for callbacks)
static JavaVM *g_jvm = nullptr;

/**
 * Initialize JNI method references
 * Called from JNI_OnLoad or when first accessed
 */
static bool InitializeJNIMethods(JNIEnv *env)
{
	// Get the AccessibilityManager class
	jclass accessibilityManagerClass = env->FindClass("org/diasurgical/devilutionx/AccessibilityManager");
	if (!accessibilityManagerClass) {
		return false;
	}

	// Get method IDs
	g_isScreenReaderEnabled = env->GetStaticMethodID(accessibilityManagerClass, "isScreenReaderEnabled", "()Z");
	if (!g_isScreenReaderEnabled) {
		return false;
	}

	// Get the AndroidTextToSpeech class
	jclass ttsClass = env->FindClass("org/diasurgical/devilutionx/AndroidTextToSpeech");
	if (!ttsClass) {
		return false;
	}

	// Get TTS method IDs
	g_speak = env->GetStaticMethodID(ttsClass, "speak", "(Ljava/lang/String;Z)V");
	g_stop = env->GetStaticMethodID(ttsClass, "stop", "()V");
	g_isReady = env->GetStaticMethodID(ttsClass, "isReady", "()Z");

	if (!g_speak || !g_stop || !g_isReady) {
		return false;
	}

	// Get the GestureDetector class
	jclass gestureClass = env->FindClass("org/diasurgical/devilutionx/GestureDetector");
	if (!gestureClass) {
		return false;
	}

	// Get gesture method ID
	g_handleGesture = env->GetStaticMethodID(gestureClass, "handleGesture", "(IFFJ)I");
	if (!g_handleGesture) {
		return false;
	}

	return true;
}

/**
 * Check if TalkBack or any accessibility service is enabled on Android
 *
 * @return true if TalkBack or accessibility is active, false otherwise
 */
bool IsAndroidAccessibilityEnabled()
{
	JNIEnv *env = nullptr;
	bool attached = false;

	// Get JNIEnv
	if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
		// Current thread is not attached, attach it
		if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
			return false;
		}
		attached = true;
	}

	// Initialize JNI methods if not already done
	if (!g_isScreenReaderEnabled) {
		if (!InitializeJNIMethods(env)) {
			if (attached) {
				g_jvm->DetachCurrentThread();
			}
			return false;
		}
	}

	// Call Java method
	jboolean result = env->CallStaticBooleanMethod(
	    env->FindClass("org/diasurgical/devilutionx/AccessibilityManager"),
	    g_isScreenReaderEnabled);

	// Detach if we attached
	if (attached) {
		g_jvm->DetachCurrentThread();
	}

	return result == JNI_TRUE;
}

/**
 * Speak text using Android's TTS engine
 *
 * @param text The text to speak
 * @param force If true, interrupt current speech
 */
void SpeakAndroidText(std::string_view text, bool force)
{
	JNIEnv *env = nullptr;
	bool attached = false;

	// Get JNIEnv
	if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
		if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
			return;
		}
		attached = true;
	}

	// Initialize JNI methods if not already done
	if (!g_speak) {
		if (!InitializeJNIMethods(env)) {
			if (attached) {
				g_jvm->DetachCurrentThread();
			}
			return;
		}
	}

	// Convert string to Java string
	jstring javaText = env->NewStringUTF(text.data());

	// Call Java method
	env->CallStaticVoidMethod(
	    env->FindClass("org/diasurgical/devilutionx/AndroidTextToSpeech"),
	    g_speak,
	    javaText,
	    static_cast<jboolean>(force));

	// Delete local reference
	env->DeleteLocalRef(javaText);

	// Detach if we attached
	if (attached) {
		g_jvm->DetachCurrentThread();
	}
}

/**
 * Stop current speech
 */
void StopAndroidSpeech()
{
	JNIEnv *env = nullptr;
	bool attached = false;

	// Get JNIEnv
	if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
		if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
			return;
		}
		attached = true;
	}

	// Initialize JNI methods if not already done
	if (!g_stop) {
		if (!InitializeJNIMethods(env)) {
			if (attached) {
				g_jvm->DetachCurrentThread();
			}
			return;
		}
	}

	// Call Java method
	env->CallStaticVoidMethod(
	    env->FindClass("org/diasurgical/devilutionx/AndroidTextToSpeech"),
	    g_stop);

	// Detach if we attached
	if (attached) {
		g_jvm->DetachCurrentThread();
	}
}

/**
 * Check if Android TTS is ready
 */
bool IsAndroidTTSReady()
{
	JNIEnv *env = nullptr;
	bool attached = false;

	// Get JNIEnv
	if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
		if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
			return false;
		}
		attached = true;
	}

	// Initialize JNI methods if not already done
	if (!g_isReady) {
		if (!InitializeJNIMethods(env)) {
			if (attached) {
				g_jvm->DetachCurrentThread();
			}
			return false;
		}
	}

	// Call Java method
	jboolean result = env->CallStaticBooleanMethod(
	    env->FindClass("org/diasurgical/devilutionx/AndroidTextToSpeech"),
	    g_isReady);

	// Detach if we attached
	if (attached) {
		g_jvm->DetachCurrentThread();
	}

	return result == JNI_TRUE;
}

/**
 * Handle touch gesture from Android
 *
 * @param action Motion event action
 * @param x X coordinate
 * @param y Y coordinate
 * @param time Event timestamp
 * @return Gesture type code
 */
int HandleAndroidGesture(int action, float x, float y, long time)
{
	JNIEnv *env = nullptr;
	bool attached = false;

	// Get JNIEnv
	if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
		if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
			return 0;
		}
		attached = true;
	}

	// Initialize JNI methods if not already done
	if (!g_handleGesture) {
		if (!InitializeJNIMethods(env)) {
			if (attached) {
				g_jvm->DetachCurrentThread();
			}
			return 0;
		}
	}

	// Call Java method
	jint result = env->CallStaticIntMethod(
	    env->FindClass("org/diasurgical/devilutionx/GestureDetector"),
	    g_handleGesture,
	    static_cast<jint>(action),
	    static_cast<jfloat>(x),
	    static_cast<jfloat>(y),
	    static_cast<jlong>(time));

	// Detach if we attached
	if (attached) {
		g_jvm->DetachCurrentThread();
	}

	return static_cast<int>(result);
}

} // namespace devilution

/**
 * JNI_OnLoad - Called when library is loaded
 * Initialize JNI environment and cache method references
 */
extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	devilution::g_jvm = vm;
	return JNI_VERSION_1_6;
}

/**
 * JNI implementation for AccessibilityManager.isScreenReaderEnabled
 * This is a wrapper that calls the C++ function
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_org_diasurgical_devilutionx_AccessibilityManager_isScreenReaderEnabled(JNIEnv *env, jclass cls)
{
	// Check if TalkBack or accessibility is enabled
	// For now, return false (will be implemented in AccessibilityManager.java)
	return JNI_FALSE;
}

/**
 * JNI implementation for AccessibilityManager.getAccessibilityServices
 * Returns list of enabled accessibility services
 */
extern "C" JNIEXPORT jstring JNICALL
Java_org_diasurgical_devilutionx_AccessibilityManager_getAccessibilityServices(JNIEnv *env, jclass cls)
{
	// Return empty string for now
	// This will be implemented in AccessibilityManager.java
	return env->NewStringUTF("");
}

/**
 * JNI implementation for AndroidTextToSpeech.speak
 */
extern "C" JNIEXPORT void JNICALL
Java_org_diasurgical_devilutionx_AndroidTextToSpeech_speak(JNIEnv *env, jclass cls, jstring text, jboolean force)
{
	if (!text) {
		return;
	}

	const char *textUtf8 = env->GetStringUTFChars(text, nullptr);
	if (textUtf8) {
		devilution::SpeakAndroidText(textUtf8, force == JNI_TRUE);
		env->ReleaseStringUTFChars(text, textUtf8);
	}
}

/**
 * JNI implementation for AndroidTextToSpeech.stop
 */
extern "C" JNIEXPORT void JNICALL
Java_org_diasurgical_devilutionx_AndroidTextToSpeech_stop(JNIEnv *env, jclass cls)
{
	devilution::StopAndroidSpeech();
}

/**
 * JNI implementation for AndroidTextToSpeech.isReady
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_org_diasurgical_devilutionx_AndroidTextToSpeech_isReady(JNIEnv *env, jclass cls)
{
	return devilution::IsAndroidTTSReady() ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI implementation for GestureDetector.handleGesture
 */
extern "C" JNIEXPORT jint JNICALL
Java_org_diasurgical_devilutionx_GestureDetector_handleGesture(JNIEnv *env, jclass cls, jint action, jfloat x, jfloat y, jlong time)
{
	return devilution::HandleAndroidGesture(
	    static_cast<int>(action),
	    static_cast<float>(x),
	    static_cast<float>(y),
	    static_cast<long>(time));
}
