# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.4.1)


set (SRC_LIST epic.cpp fake_dlfcn.cpp art.cpp)
add_library(
             epic
             SHARED
             ${SRC_LIST})

find_library(
              log-lib
              log )

target_link_libraries(
                       epic
                       ${log-lib} )
