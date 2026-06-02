package moe.koiverse.archivetune.vgm

/**
 * Kotlin wrapper for VGM decoder native library
 */
class VgmDecoder {
    companion object {
        init {
            System.loadLibrary("vgm_decoder")
        }
    }

    /**
     * VGM file information
     */
    data class VgmInfo(
        val version: Long,
        val eofOffset: Long,
        val sampleRate: Long,
        val numSamples: Long,
        val loopEnabled: Boolean,
        val loopOffset: Long,
        val loopSamples: Long,
        val gd3Offset: String = ""
    )

    private var nativeHandle: Long = 0

    /**
     * Initialize decoder with VGM file data
     */
    fun init(data: ByteArray): Boolean {
        nativeHandle = nativeInit(data)
        return nativeHandle != 0L
    }

    /**
     * Get VGM file information
     */
    fun getInfo(): VgmInfo? {
        if (nativeHandle == 0L) return null
        return nativeGetInfo(nativeHandle)
    }

    /**
     * Decode samples from VGM file
     */
    fun decode(buffer: ShortArray, maxSamples: Int): Int {
        if (nativeHandle == 0L) return 0
        return nativeDecode(nativeHandle, buffer, maxSamples)
    }

    /**
     * Seek to specific sample position
     */
    fun seek(samplePosition: Long): Boolean {
        if (nativeHandle == 0L) return false
        return nativeSeek(nativeHandle, samplePosition)
    }

    /**
     * Reset decoder state
     */
    fun reset() {
        if (nativeHandle != 0L) {
            nativeReset(nativeHandle)
        }
    }

    /**
     * Check if VGM is valid
     */
    fun isValid(): Boolean {
        if (nativeHandle == 0L) return false
        return nativeIsValid(nativeHandle)
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
    private external fun nativeInit(data: ByteArray): Long
    private external fun nativeGetInfo(handle: Long): VgmInfo?
    private external fun nativeDecode(handle: Long, buffer: ShortArray, maxSamples: Int): Int
    private external fun nativeSeek(handle: Long, samplePosition: Long): Boolean
    private external fun nativeReset(handle: Long)
    private external fun nativeIsValid(handle: Long): Boolean
    private external fun nativeClose(handle: Long)
}
