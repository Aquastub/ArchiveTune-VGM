#include "vgm_metadata.h"
#include <cstring>
#include <android/log.h>
#include <codecvt>
#include <locale>

#define LOG_TAG "VgmMetadata"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace vgm {

constexpr uint32_t GD3_MAGIC = 0x20334447; // "Gd3 " in little-endian

VgmMetadata::VgmMetadata() : parsed_(false) {
    memset(&tag_, 0, sizeof(tag_));
}

VgmMetadata::~VgmMetadata() = default;

bool VgmMetadata::parseGd3(const uint8_t* vgmData, size_t size, uint32_t gd3Offset) {
    if (!vgmData || gd3Offset >= size) {
        LOGE("Invalid GD3 data");
        return false;
    }

    // Check GD3 magic number
    uint32_t magic = (static_cast<uint32_t>(vgmData[gd3Offset]) |
                      (static_cast<uint32_t>(vgmData[gd3Offset + 1]) << 8) |
                      (static_cast<uint32_t>(vgmData[gd3Offset + 2]) << 16) |
                      (static_cast<uint32_t>(vgmData[gd3Offset + 3]) << 24));

    if (magic != GD3_MAGIC) {
        LOGD("GD3 tag not found at offset 0x%x", gd3Offset);
        return false;
    }

    size_t offset = gd3Offset + 4;

    // Skip version and size fields
    offset += 8; // Skip version (4 bytes) and size (4 bytes)

    // Parse tag strings (all in UTF-16 LE)
    size_t maxSize = size - offset;

    tag_.trackNameEn = readString(vgmData, offset, maxSize, true);
    tag_.trackNameJp = readString(vgmData, offset, maxSize, true);
    tag_.gameNameEn = readString(vgmData, offset, maxSize, true);
    tag_.gameNameJp = readString(vgmData, offset, maxSize, true);
    tag_.systemNameEn = readString(vgmData, offset, maxSize, true);
    tag_.systemNameJp = readString(vgmData, offset, maxSize, true);
    tag_.composerNameEn = readString(vgmData, offset, maxSize, true);
    tag_.composerNameJp = readString(vgmData, offset, maxSize, true);
    tag_.releaseDate = readString(vgmData, offset, maxSize, true);
    tag_.creator = readString(vgmData, offset, maxSize, true);
    tag_.notes = readString(vgmData, offset, maxSize, true);

    parsed_ = true;

    LOGD("GD3 tag parsed: track=%s, game=%s, composer=%s",
         tag_.trackNameEn.c_str(), tag_.gameNameEn.c_str(), tag_.composerNameEn.c_str());

    return true;
}

const VgmMetadata::Gd3Tag& VgmMetadata::getTag() const {
    return tag_;
}

std::map<std::string, std::string> VgmMetadata::getTagMap() const {
    std::map<std::string, std::string> map;
    map["trackName"] = tag_.trackNameEn.empty() ? tag_.trackNameJp : tag_.trackNameEn;
    map["gameName"] = tag_.gameNameEn.empty() ? tag_.gameNameJp : tag_.gameNameEn;
    map["systemName"] = tag_.systemNameEn.empty() ? tag_.systemNameJp : tag_.systemNameEn;
    map["composerName"] = tag_.composerNameEn.empty() ? tag_.composerNameJp : tag_.composerNameEn;
    map["releaseDate"] = tag_.releaseDate;
    map["creator"] = tag_.creator;
    map["notes"] = tag_.notes;
    return map;
}

std::string VgmMetadata::readString(const uint8_t* data, size_t& offset, size_t maxSize, bool unicode) {
    std::string result;

    if (unicode) {
        // UTF-16 LE string
        while (offset + 1 < maxSize) {
            uint16_t ch = (static_cast<uint16_t>(data[offset]) |
                          (static_cast<uint16_t>(data[offset + 1]) << 8));
            offset += 2;

            if (ch == 0) {
                break; // Null terminator
            }

            // Simple UTF-16 to UTF-8 conversion for ASCII range
            if (ch < 0x80) {
                result += static_cast<char>(ch);
            } else {
                // For non-ASCII, just skip (simplified)
                result += '?';
            }
        }
    } else {
        // ASCII string
        while (offset < maxSize) {
            char ch = static_cast<char>(data[offset++]);
            if (ch == 0) {
                break; // Null terminator
            }
            result += ch;
        }
    }

    return result;
}

std::string VgmMetadata::wstringToUtf8(const std::wstring& wstr) {
    if (wstr.empty()) {
        return "";
    }

    // Simple conversion for ASCII characters
    std::string result;
    for (wchar_t ch : wstr) {
        if (ch < 128) {
            result += static_cast<char>(ch);
        }
    }
    return result;
}

} // namespace vgm
