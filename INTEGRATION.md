# Diablo Access - Android Accessibility Integration Guide

## Overview

This guide provides step-by-step instructions for implementing and integrating the Android accessibility features into Diablo Access. The system includes TalkBack detection, text-to-speech (TTS), gesture-based navigation, and seamless integration with existing game code.

## Prerequisites

- Android SDK 21+ (Android 5.0 Lollipop or higher)
- CMake 3.31.0+
- NDK r28+
- Kotlin 1.9+ (for Android side)
- Existing Diablo Access codebase with SCREEN_READER_INTEGRATION enabled

## File Structure

```
diablo-access/
├── Source/
│   ├── utils/
│   │   ├── screen_reader.hpp           # Platform-agnostic interface
│   │   ├── screen_reader.cpp           # Platform implementations
│   │   ├── android_accessibility.hpp   # Android C++ interface
│   │   └── android_accessibility.cpp   # JNI bridge
│   ├── CMakeLists.txt                  # Main build config
│   └── platform/
│       └── android/
│           └── CMakeLists.txt          # Android-specific config
├── android-project/
│   └── app/
│       └── src/
│           └── main/
│               └── java/
│                   └── org/
│                       └── diasurgical/
│                           └── devilutionx/
│                               ├── AccessibilityManager.kt
│                               ├── AndroidTextToSpeech.kt
│                               ├── GestureDetector.kt
│                               ├── DevilutionXSDLActivity.java
│                               └── ...
├── ARCHITECTURE.md                     # System architecture docs
├── INTEGRATION.md                      # This file
└── README.md
```

## Step 1: Add Kotlin Files

### 1.1 Create AccessibilityManager.kt

**Location**: `android-project/app/src/main/java/org/diasurgical/devilutionx/AccessibilityManager.kt`

This file detects if TalkBack or any accessibility service is active.

**Key Features**:
- Checks `isTouchExplorationEnabled` (TalkBack navigation mode)
- Lists enabled spoken feedback services
- Provides singleton instance for JNI access

**Usage in Activity**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize accessibility manager
    AccessibilityManager.initialize(this)
}
```

### 1.2 Create AndroidTextToSpeech.kt

**Location**: `android-project/app/src/main/java/org/diasurgical/devilutionx/AndroidTextToSpeech.kt`

This file wraps Android's native Text-to-Speech engine.

**Key Features**:
- Prioritizes pt-BR (Portuguese Brazil) language
- Falls back to device default language
- Filters duplicate speech
- Thread-safe operations
- Proper resource cleanup

**Usage in Activity**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize TTS
    AndroidTextToSpeech.initialize(this)
}

override fun onDestroy() {
    super.onDestroy()

    // Shutdown TTS
    AndroidTextToSpeech.shutdown()
}
```

### 1.3 Create GestureDetector.kt

**Location**: `android-project/app/src/main/java/org/diasurgical/devilutionx/GestureDetector.kt`

This file handles custom gesture detection for accessibility navigation.

**Key Features**:
- Detects swipe left/right gestures
- Detects double-tap gestures
- Configurable thresholds
- Returns gesture codes to native code

**Gesture Codes**:
- `GESTURE_NONE = 0`
- `GESTURE_SWIPE_LEFT = 1`
- `GESTURE_SWIPE_RIGHT = 2`
- `GESTURE_DOUBLE_TAP = 3`

## Step 2: Modify DevilutionXSDLActivity.java

**Location**: `android-project/app/src/main/java/org/diasurgical/devilutionx/DevilutionXSDLActivity.java`

Add initialization calls to the existing activity:

```java
package org.diasurgical.devilutionx;

import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.View;

import org.libsdl.app.SDLActivity;

public class DevilutionXSDLActivity extends SDLActivity {
    private ExternalFilesManager fileManager;
    private boolean noExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... existing code ...

        // Initialize accessibility components
        AccessibilityManager.initialize(this);
        AndroidTextToSpeech.initialize(this);
        GestureDetector.initialize(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!noExit) {
            // Cleanup accessibility components
            AndroidTextToSpeech.shutdown();
            System.exit(0);
        }
    }
}
```

## Step 3: Add C++ Files

### 3.1 Create android_accessibility.hpp

**Location**: `Source/utils/android_accessibility.hpp`

This header declares the Android-specific accessibility functions.

**Key Functions**:
```cpp
bool IsAndroidAccessibilityEnabled();
void SpeakAndroidText(std::string_view text, bool force);
void StopAndroidSpeech();
bool IsAndroidTTSReady();
int HandleAndroidGesture(int action, float x, float y, long time);
```

### 3.2 Create android_accessibility.cpp

**Location**: `Source/utils/android_accessibility.cpp`

This file implements the JNI bridge between C++ and Kotlin.

**Key Features**:
- Caches JNI method references
- Handles thread attachment/detachment
- Converts between C++ and Java string types
- Provides JNI_OnLoad for initialization

**JNI Functions**:
```cpp
extern "C" JNIEXPORT jboolean JNICALL
Java_org_diasurgical_devilutionx_AccessibilityManager_isScreenReaderEnabled(...)

extern "C" JNIEXPORT void JNICALL
Java_org_diasurgical_devilutionx_AndroidTextToSpeech_speak(...)

extern "C" JNIEXPORT jint JNICALL
Java_org_diasurgical_devilutionx_GestureDetector_handleGesture(...)
```

## Step 4: Modify screen_reader.cpp

**Location**: `Source/utils/screen_reader.cpp`

Add Android platform support to the existing screen reader implementation:

```cpp
#include "utils/screen_reader.hpp"

#ifdef _WIN32
#include "utils/file_util.h"
#include <Tolk.h>
#elif defined(__ANDROID__)
#include "utils/android_accessibility.hpp"
#else
#include <speech-dispatcher/libspeechd.h>
#endif

namespace devilution {

#if defined(__ANDROID__)
// Android: State managed in android_accessibility.cpp
#elif !defined(_WIN32)
SPDConnection *Speechd;
#endif

void SpeakText(std::string_view text, bool force) {
    static std::string SpokenText;

    if (!force && SpokenText == text)
        return;

    SpokenText = text;

#ifdef _WIN32
    const auto textUtf16 = ToWideChar(SpokenText);
    if (textUtf16 != nullptr)
        Tolk_Output(textUtf16.get(), true);
#elif defined(__ANDROID__)
    SpeakAndroidText(SpokenText, force);
#else
    spd_say(Speechd, SPD_TEXT, SpokenText.c_str());
#endif
}

} // namespace devilution
```

## Step 5: Update CMakeLists.txt

### 5.1 Modify Source/CMakeLists.txt

**Location**: `Source/CMakeLists.txt`

Add android_accessibility.cpp to the build when on Android:

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

### 5.2 Modify Source/platform/android/CMakeLists.txt

**Location**: `Source/platform/android/CMakeLists.txt`

Ensure JNI and logging libraries are linked:

```cmake
include(functions/devilutionx_library)
add_devilutionx_object_library(libdevilutionx_android android.cpp)

# Link JNI library for Android accessibility features
find_package(log REQUIRED)

target_link_dependencies(libdevilutionx_android PUBLIC
  DevilutionX::SDL
  libdevilutionx_init
  libdevilutionx_mpq
  log
)

# Enable screen reader integration for Android
if(SCREEN_READER_INTEGRATION)
  target_compile_definitions(libdevilutionx_android PUBLIC
    __ANDROID__
  )
endif()
```

## Step 6: Build Configuration

### 6.1 Enable SCREEN_READER_INTEGRATION

Ensure the build flag is enabled:

**CMake**:
```bash
cmake -DSCREEN_READER_INTEGRATION=ON ..
```

**Gradle** (automatic, inherited from CMake)

### 6.2 Update build.gradle (if needed)

**Location**: `android-project/app/build.gradle`

No changes needed - the existing configuration supports Kotlin and JNI:

```gradle
android {
    compileSdk 35
    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 35
        externalNativeBuild {
            cmake {
                arguments "-DANDROID_STL=c++_static"
                abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
            }
        }
    }
    externalNativeBuild {
        cmake {
            path "../../CMakeLists.txt"
            version "3.31.0+"
        }
    }
}
```

## Step 7: Testing

### 7.1 Build and Deploy

```bash
# Build the Android APK/Bundle
cd android-project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "AndroidTextToSpeech|GestureDetector|AccessibilityManager|devilution"
```

### 7.2 Test TalkBack Detection

1. **Enable TalkBack**:
   - Settings → Accessibility → TalkBack → On
   - Expected: Game TTS should be disabled, system reader used

2. **Disable TalkBack**:
   - Settings → Accessibility → TalkBack → Off
   - Expected: Game TTS enabled, gestures work

3. **Test with ADB**:
```bash
# Check accessibility service status
adb shell settings get secure enabled_accessibility_services

# Check touch exploration
adb shell settings get secure touch_exploration_enabled
```

### 7.3 Test TTS Functionality

1. **Test Speech**:
   - Launch game
   - Navigate to items (should speak item names)
   - Check for pt-BR language if available

2. **Test Duplicate Filtering**:
   - Navigate to same item twice
   - Expected: Spoken only once unless forced

3. **Test Language Priority**:
   - Set device language to Portuguese (Brazil)
   - Expected: TTS speaks in Portuguese

### 7.4 Test Gesture Detection

1. **Enable Accessibility Mode**:
   - Ensure TalkBack is OFF
   - Game accessibility features should be active

2. **Test Swipe Left**:
   - Swipe left on screen
   - Expected: Navigate to next item

3. **Test Swipe Right**:
   - Swipe right on screen
   - Expected: Navigate to previous item

4. **Test Double Tap**:
   - Double tap on screen
   - Expected: Select/activate current item

5. **Log Gestures**:
```bash
adb logcat | grep GestureDetector
# Should see: "Swipe left detected", "Swipe right detected", etc.
```

### 7.5 Performance Testing

1. **Memory Leaks**:
```bash
# Monitor memory usage
adb shell dumpsys meminfo org.diasurgical.devilutionx
```

2. **JNI Overhead**:
   - Check for excessive JNI calls in logs
   - Method IDs should be cached after first use

3. **Thread Safety**:
   - Trigger multiple rapid SpeakText() calls
   - Should not crash or deadlock

## Step 8: Debugging

### 8.1 Enable Verbose Logging

**Kotlin**:
```kotlin
import android.util.Log

Log.d("AndroidTextToSpeech", "Speaking: $text")
Log.d("GestureDetector", "Gesture detected: $gestureCode")
```

**C++**:
```cpp
#include <android/log.h>

#define LOG_TAG "devilution"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

LOGD("Speaking text: %s", text.c_str());
```

### 8.2 Common Issues

**Issue**: TTS not speaking
- **Check**: Is TTS initialized? Call `AndroidTextToSpeech.isReady()`
- **Check**: Is TalkBack enabled? Should be OFF for game TTS
- **Solution**: Check logcat for TTS initialization errors

**Issue**: Gestures not working
- **Check**: Is GestureDetector initialized?
- **Check**: Are touch events reaching native code?
- **Solution**: Add logging to GestureDetector.processTouchEvent()

**Issue**: JNI crashes
- **Check**: Are method IDs valid?
- **Check**: Is JNIEnv attached to current thread?
- **Solution**: Add null checks in JNI bridge

**Issue**: Wrong language
- **Check**: Device language settings
- **Check**: Is pt-BR available on device?
- **Solution**: Test with different device languages

### 8.3 Debug Commands

```bash
# Check if accessibility services are running
adb shell service call accessibility 3

# Dump TTS engines
adb shell dumpsys texttospeech

# Check JNI libraries
adb shell ls -la /data/app/org.diasurgical.devilutionx*/lib/*/libdevilutionx.so

# Monitor all accessibility logs
adb logcat | grep -E "Accessibility|TalkBack|TTS|Gesture"
```

## Step 9: Deployment

### 9.1 Release Checklist

- [ ] Build release APK/AAB
- [ ] Test on multiple Android versions (5.0, 8.0, 11, 14)
- [ ] Test on different screen sizes
- [ ] Test with TalkBack enabled/disabled
- [ ] Test gesture detection on various devices
- [ ] Verify pt-BR language support
- [ ] Memory leak testing
- [ ] Performance profiling

### 9.2 Accessibility Settings

Document in user manual:

1. **TalkBack Mode**:
   - When ON: Use system screen reader
   - When OFF: Use game accessibility features

2. **Gesture Navigation**:
   - Swipe Left: Next item
   - Swipe Right: Previous item
   - Double Tap: Select

3. **Language Support**:
   - Prioritizes Portuguese Brazilian (pt-BR)
   - Falls back to device default

### 9.3 User Documentation

Update README.md with Android-specific information:

```markdown
## Android Accessibility

Diablo Access on Android includes full accessibility support:

- Automatic TalkBack detection
- Native text-to-speech integration
- Gesture-based navigation
- Portuguese Brazilian language priority

### Requirements

- Android 5.0 (Lollipop) or higher
- Screen reader (optional): TalkBack or other accessibility service

### Controls

- **Swipe Left**: Navigate to next item/target
- **Swipe Right**: Navigate to previous item/target
- **Double Tap**: Select/activate current item
```

## Step 10: Maintenance

### 10.1 Regular Updates

- **Test new Android versions**: Each major Android release
- **API deprecations**: Check for deprecated APIs
- **TTS engine changes**: Verify TTS behavior on new Android versions
- **Accessibility services**: Test with new accessibility features

### 10.2 Monitoring

- **Crash reports**: Monitor Google Play Console for crashes
- **User feedback**: Check accessibility-specific issues
- **Performance**: Monitor memory and CPU usage

### 10.3 Future Enhancements

Potential improvements:

1. **Settings UI**: In-game accessibility settings
2. **Gesture customization**: Let users adjust gesture sensitivity
3. **Haptic feedback**: Vibration on gesture recognition
4. **More gestures**: Long press, two-finger gestures
5. **Voice selection**: Choose TTS voice
6. **Speed control**: Adjustable speech rate

## Conclusion

The Android accessibility system is now fully integrated. The modular design ensures clean separation between game logic and accessibility code, making it easy to maintain and extend.

For detailed architecture information, see `ARCHITECTURE.md`.

For support or issues, please refer to the project's issue tracker.
