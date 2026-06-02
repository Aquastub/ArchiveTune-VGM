cmake_minimum_required(VERSION 3.22.1)
project(vgm_decoder)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add source files
set(SOURCES
    src/vgm_decoder.cpp
    src/vgm_metadata.cpp
)

# Create shared library
add_library(vgm_decoder SHARED ${SOURCES})

# Include directories
target_include_directories(vgm_decoder PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Compiler flags
target_compile_options(vgm_decoder PRIVATE
    -Wall
    -Wextra
    -Werror=format
    -fvisibility=hidden
)

# Link libraries
target_link_libraries(vgm_decoder
    log
    android
)

# Optional: Add libvgm as a dependency (for future integration)
# If building libvgm separately, uncomment:
# find_package(vgm REQUIRED)
# target_link_libraries(vgm_decoder vgm::vgm)
