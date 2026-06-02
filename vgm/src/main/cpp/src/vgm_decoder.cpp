#include "vgm_decoder.h"
#include <cstring>
#include <android/log.h>

#define LOG_TAG "VgmDecoder"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace vgm {

constexpr uint32_t VGM_MAGIC = 0x206D6756; // "Vgm " in little-endian

VgmDecoder::VgmDecoder()
    : data_(nullptr),
      dataSize_(0),
      currentSample_(0),
      initialized_(false) {
    memset(&info_, 0, sizeof(info_));
    info_.sampleRate = 44100; // Default sample rate
}

VgmDecoder::~VgmDecoder() = default;

bool VgmDecoder::init(const uint8_t* data, size_t size) {
    if (!data || size < 0x40) {
        LOGE("Invalid VGM data: nullptr or too small");
        return false;
    }

    data_ = data;
    dataSize_ = size;

    // Verify magic number
    if (readUint32(0) != VGM_MAGIC) {
        LOGE("Invalid VGM magic number");
        return false;
    }

    parseHeader();
    initialized_ = true;

    LOGD("VGM initialized: version=0x%x, samples=%u, sampleRate=%u",
         info_.version, info_.numSamples, info_.sampleRate);

    return true;
}

const VgmDecoder::VgmInfo& VgmDecoder::getInfo() const {
    return info_;
}

size_t VgmDecoder::decode(int16_t* buffer, size_t maxSamples) {
    if (!initialized_ || !buffer) {
        return 0;
    }

    // TODO: Implement actual VGM data parsing and decoding
    // For now, return silence as placeholder
    memset(buffer, 0, maxSamples * sizeof(int16_t));

    size_t samplesToRead = std::min(maxSamples, 
                                    (size_t)(info_.numSamples - currentSample_));
    currentSample_ += samplesToRead;

    return samplesToRead;
}

bool VgmDecoder::seek(uint32_t samplePosition) {
    if (samplePosition > info_.numSamples) {
        return false;
    }

    currentSample_ = samplePosition;
    return true;
}

void VgmDecoder::reset() {
    currentSample_ = 0;
}

bool VgmDecoder::isValid() const {
    return initialized_ && info_.numSamples > 0;
}

void VgmDecoder::parseHeader() {
    // VGM header structure (offset 0x00-0x3F minimum)
    // 0x00-0x03: Magic "Vgm "
    // 0x04-0x07: EOF offset
    // 0x08-0x0B: Version number
    // 0x0C-0x0F: SN76489 clock
    // 0x10-0x13: YM2413 clock
    // 0x14-0x17: GD3 offset
    // 0x18-0x1B: Total # of samples
    // 0x1C-0x1F: Loop offset
    // 0x20-0x23: Loop # of samples

    info_.version = readUint32(0x08);
    info_.eofOffset = readUint32(0x04) + 0x04; // EOF offset is relative
    info_.sampleRate = 44100; // Standard VGM sample rate
    info_.numSamples = readUint32(0x18);
    info_.loopOffset = readUint32(0x1C);
    info_.loopSamples = readUint32(0x20);
    info_.loopEnabled = info_.loopOffset > 0;

    // GD3 tag offset (v1.10+)
    uint32_t gd3Offset = readUint32(0x14);
    if (gd3Offset > 0) {
        info_.gd3Offset = std::to_string(gd3Offset + 0x14);
    }

    LOGD("VGM Header parsed: version=0x%x, eofOffset=0x%x, numSamples=%u, loopEnabled=%d",
         info_.version, info_.eofOffset, info_.numSamples, info_.loopEnabled);
}

void VgmDecoder::parseGd3() {
    // TODO: Implement GD3 tag parsing
    // GD3 tags contain metadata like title, game name, composer, etc.
}

uint32_t VgmDecoder::readUint32(size_t offset) const {
    if (offset + 4 > dataSize_) {
        return 0;
    }
    return (static_cast<uint32_t>(data_[offset]) |
            (static_cast<uint32_t>(data_[offset + 1]) << 8) |
            (static_cast<uint32_t>(data_[offset + 2]) << 16) |
            (static_cast<uint32_t>(data_[offset + 3]) << 24));
}

uint16_t VgmDecoder::readUint16(size_t offset) const {
    if (offset + 2 > dataSize_) {
        return 0;
    }
    return (static_cast<uint16_t>(data_[offset]) |
            (static_cast<uint16_t>(data_[offset + 1]) << 8));
}

uint8_t VgmDecoder::readUint8(size_t offset) const {
    if (offset >= dataSize_) {
        return 0;
    }
    return data_[offset];
}

} // namespace vgm
