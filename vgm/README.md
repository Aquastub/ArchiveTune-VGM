# VGM Support README

This directory contains the VGM (Video Game Music) decoder module for ArchiveTune.

## Overview

The VGM module provides support for playing `.vgm` and `.vgz` (compressed VGM) files in ArchiveTune. It includes:

- **VGM File Format Support**: Parsing VGM headers and extracting metadata
- **GD3 Tag Extraction**: Reading game, composer, and track information from VGM files
- **JNI Wrappers**: Kotlin bindings to native C++ code for performance

## Module Structure

```
vgm/
├── build.gradle.kts          # Module build configuration with NDK support
├── src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt     # CMake configuration for native compilation
│   │   ├── include/
│   │   │   ├── vgm_decoder.h  # VGM decoder header
│   │   │   └── vgm_metadata.h # GD3 metadata parser header
│   │   └── src/
│   │       ├── vgm_decoder.cpp     # VGM decoder implementation
│   │       └── vgm_metadata.cpp    # GD3 metadata parser implementation
│   └── kotlin/moe/koiverse/archivetune/vgm/
│       ├── VgmDecoder.kt      # Kotlin JNI wrapper for decoder
│       └── VgmMetadata.kt     # Kotlin JNI wrapper for metadata
```

## Features

### VGM Decoder (C++)
- Parses VGM file headers
- Extracts audio sample count, loop information, and clock frequencies
- Provides seek and reset functionality
- Supports VGM versions 1.10+

### GD3 Tag Parser
- Reads UTF-16 encoded metadata from GD3 tags
- Extracts:
  - Track name (English & Japanese)
  - Game name (English & Japanese)
  - System name (English & Japanese)
  - Composer name (English & Japanese)
  - Release date
  - Creator/Author
  - Notes

### Kotlin API
Clean Kotlin wrappers for easy integration with the app:

```kotlin
// Initialize decoder
val decoder = VgmDecoder()
val success = decoder.init(vgmData)

// Get file info
val info = decoder.getInfo()
println("Samples: ${info?.numSamples}, Sample Rate: ${info?.sampleRate}")

// Parse metadata
val metadata = VgmMetadata()
metadata.parseGd3(vgmData, gd3Offset.toLong())
println("Composer: ${metadata.composerNameEn}")

// Decode audio (placeholder)
val audioBuffer = ShortArray(4096)
val samplesRead = decoder.decode(audioBuffer, 4096)
```

## Future Work

1. **Actual Audio Decoding**: Implement chip emulation (SN76489, YM2413, YM2612, etc.)
2. **LibVGM Integration**: Link against libvgm for comprehensive chip support
3. **ExoPlayer Integration**: Create custom MediaSource for seamless playback
4. **Loop Handling**: Implement proper loop point handling during playback
5. **Compression Support**: Add .vgz decompression

## Building

The module is automatically included in the project build. To build manually:

```bash
./gradlew :vgm:build
```

For NDK compilation:
```bash
./gradlew :vgm:assembleDebug
```

## Integration with App

To use the VGM module in the main app:

```gradle
dependencies {
    implementation(project(":vgm"))
}
```

Then use it in your playback code:

```kotlin
import moe.koiverse.archivetune.vgm.VgmDecoder
import moe.koiverse.archivetune.vgm.VgmMetadata

// Create decoder instance
val vgmDecoder = VgmDecoder()
```

## References

- [VGM Format Specification](https://www.smspower.org/Development/VGMFormat)
- [LibVGM Project](https://github.com/vgmrips/libvgm)
- [GD3 Tag Specification](https://www.smspower.org/Development/GD3)
