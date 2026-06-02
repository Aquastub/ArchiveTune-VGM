#pragma once

#include <jni.h>
#include <vector>
#include <cstdint>
#include <memory>
#include <string>

namespace vgm {

/**
 * VGM Decoder for Android
 * Handles VGM (Video Game Music) file format decoding
 */
class VgmDecoder {
public:
    struct VgmInfo {
        uint32_t version;
        uint32_t dataOffset;
        uint32_t eofOffset;
        uint32_t sampleRate;
        uint32_t numSamples;
        std::string gd3Offset;
        bool loopEnabled;
        uint32_t loopOffset;
        uint32_t loopSamples;
    };

    VgmDecoder();
    ~VgmDecoder();

    /**
     * Initialize decoder with VGM file data
     */
    bool init(const uint8_t* data, size_t size);

    /**
     * Get VGM file information
     */
    const VgmInfo& getInfo() const;

    /**
     * Decode samples from VGM file
     * Returns number of samples decoded
     */
    size_t decode(int16_t* buffer, size_t maxSamples);

    /**
     * Seek to specific sample position
     */
    bool seek(uint32_t samplePosition);

    /**
     * Reset decoder state
     */
    void reset();

    /**
     * Check if VGM file is valid
     */
    bool isValid() const;

private:
    VgmInfo info_;
    const uint8_t* data_;
    size_t dataSize_;
    uint32_t currentSample_;
    bool initialized_;

    // Helper methods
    void parseHeader();
    void parseGd3();
    uint32_t readUint32(size_t offset) const;
    uint16_t readUint16(size_t offset) const;
    uint8_t readUint8(size_t offset) const;
};

} // namespace vgm
