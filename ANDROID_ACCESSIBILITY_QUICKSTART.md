# Android Accessibility Quick Start

## What Was Created

### Kotlin Files (android-project/app/src/main/java/org/diasurgical/devilutionx/)

1. **AccessibilityManager.kt** (4.1 KB)
   - Detects TalkBack and accessibility services
   - Provides isScreenReaderActive() for game logic

2. **AndroidTextToSpeech.kt** (9.8 KB)
   - Wraps Android's native TTS engine
   - Prioritizes pt-BR language
   - Filters duplicate speech
   - Thread-safe operations

3. **GestureDetector.kt** (12 KB)
   - Custom gesture detection for accessibility
   - Supports: Swipe Left, Swipe Right, Double Tap
   - Configurable thresholds

### C++ Files (Source/utils/)

4. **android_accessibility.hpp** (1.9 KB)
   - Declares Android-specific accessibility functions
   - Gesture type constants

5. **android_accessibility.cpp** (8.9 KB)
   - JNI bridge between C++ and Kotlin
   - Handles thread attachment/detachment
   - Caches JNI method references

### Modified Files

6. **Source/utils/screen_reader.cpp**
   - Added Android platform support (#ifdef __ANDROID__)
   - Routes SpeakText() calls to Android TTS

7. **Source/CMakeLists.txt**
   - Added android_accessibility.cpp to build when ANDROID=ON

8. **Source/platform/android/CMakeLists.txt**
   - Linked log library for Android logging

### Documentation

9. **ARCHITECTURE.md** (15 KB)
   - Complete system architecture documentation
   - Data flow diagrams
   - Component details

10. **INTEGRATION.md** (16 KB)
    - Step-by-step implementation guide
    - Testing procedures
    - Debugging guide

## How It Works

```
Game Code (C++)
    │
    ├── SpeakText("Hello") ──► screen_reader.cpp
    │                              │
    │                         #ifdef __ANDROID__
    │                              │
    │                         SpeakAndroidText()
    │                              │
    └──────────────────────────────┼────► JNI Call
                                   │
                          AndroidTextToSpeech.kt
                                   │
                            tts.speak("Hello")
                                   │
                            Android TTS Engine
                                   │
                              Audio Output
```

## Key Features

✅ **Automatic TalkBack Detection**
- When TalkBack ON: Uses system screen reader
- When TalkBack OFF: Uses game's built-in TTS

✅ **Native Android TTS**
- No external libraries needed
- Uses Android's TextToSpeech API
- Prioritizes pt-BR language

✅ **Gesture Navigation**
- Swipe Left: Next item
- Swipe Right: Previous item
- Double Tap: Select

✅ **Modular Design**
- Fully separate from game logic
- Reuses existing SpeakText() interface
- Platform-agnostic abstraction

## Build Instructions

```bash
# Enable screen reader integration
cmake -DSCREEN_READER_INTEGRATION=ON ..

# Build Android APK
cd android-project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Testing Checklist

- [ ] TalkBack ON: System reader used, game TTS disabled
- [ ] TalkBack OFF: Game TTS enabled, gestures work
- [ ] pt-BR language: TTS speaks Portuguese if available
- [ ] Duplicate text: Not spoken twice
- [ ] Swipe gestures: Navigate correctly
- [ ] Double tap: Selects items
- [ ] Memory: No leaks after extended play

## Integration Status

✅ Kotlin files created
✅ JNI bridge implemented
✅ screen_reader.cpp modified for Android
✅ CMakeLists.txt updated
✅ Documentation complete

## Next Steps

1. Test on physical Android device
2. Verify TalkBack detection works
3. Test gesture recognition
4. Check pt-BR language support
5. Run memory leak detection
6. Performance testing

## Support

For detailed information:
- See ARCHITECTURE.md for system design
- See INTEGRATION.md for implementation guide
- See Source/utils/screen_reader.cpp for usage examples

## File Locations

```
diablo-access/
├── Source/utils/
│   ├── screen_reader.hpp          # Platform-agnostic interface
│   ├── screen_reader.cpp          # Modified for Android
│   ├── android_accessibility.hpp  # Android C++ interface
│   └── android_accessibility.cpp  # JNI bridge
│
├── android-project/app/src/main/java/org/diasurgical/devilutionx/
│   ├── AccessibilityManager.kt    # TalkBack detection
│   ├── AndroidTextToSpeech.kt     # TTS wrapper
│   └── GestureDetector.kt         # Gesture handling
│
├── ARCHITECTURE.md                # System architecture
├── INTEGRATION.md                 # Implementation guide
└── ANDROID_ACCESSIBILITY_QUICKSTART.md  # This file
```

## Quick Test

```bash
# Enable logging
adb shell setprop log.tag.AndroidTextToSpeech DEBUG
adb shell setprop log.tag.GestureDetector DEBUG

# Monitor logs
adb logcat | grep -E "AndroidTextToSpeech|GestureDetector|AccessibilityManager"

# Check TalkBack status
adb shell settings get secure touch_exploration_enabled
```

## Summary

Total files created: **10**
Total lines of code: **~1,500**
Languages used: Kotlin, C++, Java
Documentation: Complete

The Android accessibility system is **ready for testing**!
