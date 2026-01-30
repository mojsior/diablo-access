# Diablo Access - Android Accessibility Architecture

## Overview

This document describes the complete architecture of the Android accessibility implementation for Diablo Access. The system provides screen reader integration, text-to-speech (TTS), gesture-based navigation, and automatic detection of TalkBack to enable seamless accessibility for blind and visually impaired players.

## Design Goals

1. **Non-intrusive Integration**: The accessibility system is fully modular and separate from game logic
2. **Automatic Detection**: Automatically detects if TalkBack is active and switches modes accordingly
3. **Native Implementation**: Uses Android's built-in TTS (no external libraries)
4. **Gesture Support**: Provides intuitive swipe gestures for navigation
5. **Language Priority**: Prioritizes Portuguese Brazilian (pt-BR) language
6. **Performance**: Efficient speech queuing and duplicate text filtering

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Diablo Access Game                       │
│                  (C++ Game Logic)                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ SpeakText("text")
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   screen_reader.cpp                          │
│              (Platform Abstraction Layer)                    │
│  - InitializeScreenReader()                                  │
│  - SpeakText(text, force)                                    │
│  - ShutDownScreenReader()                                    │
└──────┬─────────────────────────────────────────────────────┬┘
       │                                                     │
       │ #ifdef _WIN32                                      │ │ __ANDROID__
       │                                                     │
       ▼                                                     ▼
┌────────────────────┐                          ┌──────────────────────────┐
│   Windows (Tolk)   │                          │   Android Implementation │
│  - NVDA/JAWS       │                          │  (android_accessibility)  │
└────────────────────┘                          └───────────┬──────────────┘
                                                           │
                                    ┌──────────────────────┼──────────────────────┐
                                    │                      │                      │
                                    ▼                      ▼                      ▼
                          ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐
                          │AccessibilityMgr │  │  AndroidTextToSpeech │ │  GestureDetector│
                          │     (Kotlin)    │  │     (Kotlin)     │  │    (Kotlin)     │
                          └────────┬────────┘  └────────┬─────────┘  └────────┬────────┘
                                   │                     │                       │
                                   ▼                     ▼                       ▼
                          ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐
                          │ Android APIs    │  │  Android TTS     │  │  Touch Events   │
                          │TalkBack Detect  │  │  Engine          │  │  Processing     │
                          └─────────────────┘  └──────────────────┘  └─────────────────┘
```

## Component Details

### 1. Platform Abstraction Layer (screen_reader.hpp/cpp)

**File**: `Source/utils/screen_reader.hpp`, `Source/utils/screen_reader.cpp`

**Purpose**: Provides a platform-agnostic interface for screen reader functionality

**Key Functions**:
- `InitializeScreenReader()`: Initialize the appropriate screen reader for the platform
- `SpeakText(text, force)`: Speak text through the screen reader
- `ShutDownScreenReader()`: Clean up resources

**Implementation**:
```cpp
#ifdef _WIN32
    // Use Tolk library for NVDA/JAWS
#elif defined(__ANDROID__)
    // Use Android accessibility system
#else
    // Use Speech Dispatcher for Linux
#endif
```

### 2. Android Accessibility Implementation

#### 2.1 AccessibilityManager.kt

**File**: `android-project/app/src/main/java/org/diasurgical/devilutionx/AccessibilityManager.kt`

**Purpose**: Detect if TalkBack or any accessibility service is active

**Key Methods**:
- `isScreenReaderActive()`: Check if TalkBack is enabled
- `getEnabledAccessibilityServiceNames()`: List enabled services (debugging)

**Detection Logic**:
1. Check `AccessibilityManager.isTouchExplorationEnabled` (TalkBack navigation)
2. Check for enabled spoken feedback services
3. Return `true` if either is active

**Usage**:
```kotlin
val isTalkbackEnabled = AccessibilityManager.isScreenReaderActive()
if (isTalkbackEnabled) {
    // Use system screen reader - disable game TTS
} else {
    // Enable game's built-in accessibility features
}
```

#### 2.2 AndroidTextToSpeech.kt

**File**: `android-project/app/src/main/java/org/diasurgical/devilutionx/AndroidTextToSpeech.kt`

**Purpose**: Wrapper for Android's native Text-to-Speech engine

**Key Features**:
- **Language Priority**: Attempts pt-BR first, falls back to pt, then device default
- **Duplicate Filtering**: Won't speak same text twice unless forced
- **Speech Queueing**: Manages speech queue to prevent buildup
- **Thread Safety**: Synchronized speech operations

**Initialization**:
```kotlin
tts = TextToSpeech(context, this) // Calls onInit when ready

override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        // Try pt-BR first
        val ptBR = Locale("pt", "BR")
        tts.setLanguage(ptBR)

        // Configure speech parameters
        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)
    }
}
```

**Speech Operation**:
```kotlin
fun speakText(text: String, force: Boolean) {
    // Skip duplicates if not forced
    if (!force && text == lastSpokenText) return

    // Clean text (remove null chars)
    val cleanText = text.replace("\u0000", "").trim()

    // Speak with appropriate queue mode
    val queueMode = if (force) QUEUE_FLUSH else QUEUE_ADD
    tts.speak(cleanText, queueMode, params, utteranceId)
}
```

#### 2.3 GestureDetector.kt

**File**: `android-project/app/src/main/java/org/diasurgical/devilutionx/GestureDetector.kt`

**Purpose**: Custom gesture detection for accessibility navigation

**Supported Gestures**:
- **Swipe Left**: Navigate to next item/target
- **Swipe Right**: Navigate to previous item/target
- **Double Tap**: Select/activate current item

**Gesture Detection Algorithm**:
1. Record first tap position and time
2. On second tap, check distance and time delta
3. If close (< 100px) and fast (< 300ms): Double tap
4. If horizontal movement > 100px: Swipe gesture
5. Return gesture code to native code

**Thresholds**:
```kotlin
const val SWIPE_MIN_DISTANCE = 100        // pixels
const val SWIPE_THRESHOLD_VELOCITY = 100  // pixels/second
const val DOUBLE_TAP_TIMEOUT = 300L       // milliseconds
const val DOUBLE_TAP_DISTANCE = 100       // pixels
```

### 3. JNI Bridge

#### 3.1 android_accessibility.cpp/hpp

**Files**:
- `Source/utils/android_accessibility.cpp`
- `Source/utils/android_accessibility.hpp`

**Purpose**: Bridge between C++ game code and Kotlin accessibility classes

**Key Functions**:

**C++ → Java (called from game)**:
```cpp
// Forward speech request to Kotlin
void SpeakAndroidText(std::string_view text, bool force) {
    JNIEnv* env = GetJNIEnv();
    jstring javaText = env->NewStringUTF(text.data());
    env->CallStaticVoidMethod(ttsClass, g_speak, javaText, force);
}

// Check accessibility status
bool IsAndroidAccessibilityEnabled() {
    return CallJavaBooleanMethod("isScreenReaderEnabled");
}

// Handle gesture events
int HandleAndroidGesture(int action, float x, float y, long time) {
    return CallJavaIntMethod("handleGesture", action, x, y, time);
}
```

**Java → C++ (JNI callbacks)**:
```cpp
extern "C" JNIEXPORT void JNICALL
Java_org_diasurgical_devilutionx_AndroidTextToSpeech_speak(
    JNIEnv *env, jclass cls, jstring text, jboolean force) {
    // Forward to C++ implementation
    const char *textUtf8 = env->GetStringUTFChars(text, nullptr);
    SpeakAndroidText(textUtf8, force == JNI_TRUE);
    env->ReleaseStringUTFChars(text, textUtf8);
}
```

**JNI OnLoad**:
```cpp
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    // Store JVM reference for later use
    g_jvm = vm;
    return JNI_VERSION_1_6;
}
```

## Data Flow

### Speech Flow

```
Game Code (C++)
    │
    │ SpeakText("Hello, world")
    │
    ▼
screen_reader.cpp::SpeakText()
    │
    │ #ifdef __ANDROID__
    │
    ▼
android_accessibility.cpp::SpeakAndroidText()
    │
    │ JNI Call
    │
    ▼
AndroidTextToSpeech.kt::speakText()
    │
    │ tts.speak(text, queueMode, params)
    │
    ▼
Android TTS Engine
    │
    ▼
Audio Output
```

### Gesture Flow

```
User Touch Screen
    │
    ▼
SDL Touch Event
    │
    │ Pass to native code
    │
    ▼
HandleAndroidGesture(action, x, y, time)
    │
    │ JNI Call
    │
    ▼
GestureDetector.kt::processTouchEvent()
    │
    │ Analyze gesture pattern
    │
    ▼
Return Gesture Code (SWIPE_LEFT, SWIPE_RIGHT, DOUBLE_TAP)
    │
    ▼
Game Code (C++)
    │
    │ Execute action
    │
    ▼
Speak result or update state
```

## Platform Detection Logic

The system uses a cascading detection approach:

1. **Game Start**: `InitializeScreenReader()` is called
2. **Android**: Kotlin classes initialize in `DevilutionXSDLActivity.onCreate()`
3. **TalkBack Check**: `AccessibilityManager.isScreenReaderActive()`
   - If TalkBack ON: Use system accessibility (game TTS disabled)
   - If TalkBack OFF: Enable game's TTS and gestures
4. **Runtime**: All `SpeakText()` calls route through appropriate system

## Memory Management

### C++ Side
- Singleton pattern for JNI references
- Static method IDs cached after first use
- JNIEnv attached/detached per call

### Kotlin Side
- Application context (not activity context) prevents leaks
- TTS properly shut down in `onDestroy()`
- Weak references where appropriate

## Thread Safety

### TTS Operations
- `speechQueueLock` mutex protects speech queue
- Synchronized access to `lastSpokenText`
- Thread-safe JNI calls with proper attachment

### Gesture Detection
- First tap state protected by instance variables
- Reset on game pause/exit

## Performance Considerations

1. **JNI Overhead**: Method IDs cached after first call
2. **Text Filtering**: Duplicate detection prevents redundant speech
3. **Queue Management**: Proper flush vs add mode selection
4. **Language Detection**: One-time check on initialization

## Build Configuration

### CMakeLists.txt Changes

**Source/CMakeLists.txt**:
```cmake
if(SCREEN_READER_INTEGRATION)
  list(APPEND libdevilutionx_SRCS
    utils/screen_reader.cpp
  )
  if(ANDROID)
    list(APPEND libdevilutionx_SRCS
      utils/android_accessibility.cpp
    )
  endif()
endif()
```

**Source/platform/android/CMakeLists.txt**:
```cmake
target_link_dependencies(libdevilutionx_android PUBLIC
  DevilutionX::SDL
  libdevilutionx_init
  libdevilutionx_mpq
  log  # Android logging library
)
```

## Testing Checklist

- [ ] TalkBack ON: Game TTS disabled, system reader used
- [ ] TalkBack OFF: Game TTS enabled, gestures work
- [ ] pt-BR language: TTS speaks Portuguese if available
- [ ] Duplicate text: Not spoken twice unless forced
- [ ] Gesture swipe left: Previous item navigation
- [ ] Gesture swipe right: Next item navigation
- [ ] Gesture double tap: Selection/activation
- [ ] Memory leaks: None on long play sessions
- [ ] Thread safety: No crashes from concurrent calls

## Future Enhancements

1. **Settings Screen**: Allow gesture threshold customization
2. **Voice Selection**: Let users choose TTS voice
3. **Speed Control**: Adjustable speech rate
4. **More Gestures**: Long press, two-finger swipe, etc.
5. **Haptic Feedback**: Vibration on gesture recognition
6. **Speech History**: Log of spoken text for debugging

## Related Files

- `INTEGRATION.md`: Step-by-step implementation guide
- `README.md`: Project overview and features
- `Source/utils/screen_reader.hpp`: Platform-agnostic interface
- `Source/utils/screen_reader.cpp`: Platform implementations
- `Source/utils/android_accessibility.hpp`: Android C++ interface
- `Source/utils/android_accessibility.cpp`: JNI bridge implementation

## Conclusion

The Android accessibility system provides a complete, modular solution for screen reader integration. By leveraging Android's native APIs and maintaining clean separation between game logic and accessibility code, the system ensures a smooth experience for blind and visually impaired players.
