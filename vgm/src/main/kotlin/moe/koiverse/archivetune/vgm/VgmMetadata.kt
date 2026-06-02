package moe.koiverse.archivetune.vgm

/**
 * VGM metadata and GD3 tag information
 */
data class VgmMetadata(
    val trackNameEn: String = "",
    val trackNameJp: String = "",
    val gameNameEn: String = "",
    val gameNameJp: String = "",
    val systemNameEn: String = "",
    val systemNameJp: String = "",
    val composerNameEn: String = "",
    val composerNameJp: String = "",
    val releaseDate: String = "",
    val creator: String = "",
    val notes: String = ""
) {
    companion object {
        init {
            System.loadLibrary("vgm_decoder")
        }
    }

    private var nativeHandle: Long = 0

    /**
     * Parse GD3 tag from VGM data
     */
    fun parseGd3(vgmData: ByteArray, gd3Offset: Long): Boolean {
        nativeHandle = nativeParseGd3(vgmData, gd3Offset)
        return nativeHandle != 0L
    }

    /**
     * Get metadata as map
     */
    fun getTagMap(): Map<String, String> {
        return mapOf(
            "trackName" to if (trackNameEn.isNotEmpty()) trackNameEn else trackNameJp,
            "gameName" to if (gameNameEn.isNotEmpty()) gameNameEn else gameNameJp,
            "systemName" to if (systemNameEn.isNotEmpty()) systemNameEn else systemNameJp,
            "composerName" to if (composerNameEn.isNotEmpty()) composerNameEn else composerNameJp,
            "releaseDate" to releaseDate,
            "creator" to creator,
            "notes" to notes
        )
    }

    /**
     * Get preferred language metadata
     */
    fun getDisplayMetadata(): Map<String, String> {
        return mapOf(
            "title" to (trackNameEn.ifEmpty { trackNameJp }),
            "game" to (gameNameEn.ifEmpty { gameNameJp }),
            "system" to (systemNameEn.ifEmpty { systemNameJp }),
            "composer" to (composerNameEn.ifEmpty { composerNameJp }),
            "releaseDate" to releaseDate,
            "artist" to creator
        )
    }

    /**
     * Clean up resources
     */
    fun close() {
        if (nativeHandle != 0L) {
            nativeClose(nativeHandle)
            nativeHandle = 0L
        }
    }

    // Native methods (JNI)
    private external fun nativeParseGd3(vgmData: ByteArray, gd3Offset: Long): Long
    private external fun nativeClose(handle: Long)
}
