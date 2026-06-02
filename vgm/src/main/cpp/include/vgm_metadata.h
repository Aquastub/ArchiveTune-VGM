#pragma once

#include <string>
#include <map>
#include <memory>
#include <cstdint>

namespace vgm {

/**
 * GD3 Tag Parser for VGM files
 * Extracts metadata like title, game, composer, etc.
 */
class VgmMetadata {
public:
    struct Gd3Tag {
        std::string trackNameEn;
        std::string trackNameJp;
        std::string gameNameEn;
        std::string gameNameJp;
        std::string systemNameEn;
        std::string systemNameJp;
        std::string composerNameEn;
        std::string composerNameJp;
        std::string releaseDate;
        std::string creator;
        std::string notes;
    };

    VgmMetadata();
    ~VgmMetadata();

    /**
     * Parse GD3 tag from VGM data
     */
    bool parseGd3(const uint8_t* vgmData, size_t size, uint32_t gd3Offset);

    /**
     * Get parsed GD3 tag
     */
    const Gd3Tag& getTag() const;

    /**
     * Get tag as string map
     */
    std::map<std::string, std::string> getTagMap() const;

private:
    Gd3Tag tag_;
    bool parsed_;

    // Helper methods
    std::string readString(const uint8_t* data, size_t& offset, size_t maxSize, bool unicode = true);
    std::string wstringToUtf8(const std::wstring& wstr);
};

} // namespace vgm
