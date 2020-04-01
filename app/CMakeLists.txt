# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/native-lib.cpp
        src/main/cpp/membuf.cpp
        src/main/cpp/adpcm.cpp
        src/main/cpp/md5.c
        src/main/cpp/soundbuf.cpp
        src/main/cpp/cmucom.cpp
        src/main/cpp/mucomvm.cpp
        src/main/cpp/mucomerror.cpp
        src/main/cpp/Z80/Z80.cpp
        src/main/cpp/fmgen/file.cpp
        src/main/cpp/fmgen/fmgen.cpp
        src/main/cpp/fmgen/fmtimer.cpp
        src/main/cpp/fmgen/opm.cpp
        src/main/cpp/fmgen/opna.cpp
        src/main/cpp/fmgen/psg.cpp
        src/main/cpp/callback.cpp
        src/main/cpp/osdep.cpp
        src/main/cpp/utils/s98write.cpp
        src/main/cpp/utils/vgmwrite.cpp
        src/main/cpp/utils/wavwrite.cpp
        src/main/cpp/dummy/osdep_dummy.cpp
        src/main/cpp/module/mucom_module.cpp
        )

    target_include_directories(native-lib PRIVATE
        src/main/cpp
        src/main/cpp/module
        src/main/cpp/dummy
        src/main/cpp/Z80
        src/main/cpp/fmgen
    )


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})