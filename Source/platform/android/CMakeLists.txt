include(functions/devilutionx_library)
add_devilutionx_object_library(libdevilutionx_android android.cpp)

# Link JNI library for Android accessibility features (NDK log)
find_library(log-lib log)

target_link_libraries(libdevilutionx_android PUBLIC
  DevilutionX::SDL
  libdevilutionx_init
  libdevilutionx_mpq
  ${log-lib}
)

# Enable screen reader integration for Android
if(SCREEN_READER_INTEGRATION)
  target_compile_definitions(libdevilutionx_android PUBLIC
    __ANDROID__
  )
endif()
