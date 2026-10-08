package com.shilapi.xcertplay.media

/** Turns the microphone up or down before it is sent to the iPhone. Ported from xcertplay acbef114. */
object MicrophoneGain {
    const val MIN_PERCENT = 80
    const val MAX_PERCENT = 200
    const val DEFAULT_PERCENT = 100
    const val STEP_PERCENT = 10

    /** The levels Settings offers. */
    val percents: List<Int> = (MIN_PERCENT..MAX_PERCENT step STEP_PERCENT).toList()

    fun sanitize(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    /** Scales the first [count] bytes of 16-bit little-endian PCM in [buffer], clipping at full scale. */
    internal fun applyPcm16InPlace(buffer: ByteArray, count: Int, percent: Int) {
        require(count in 0..buffer.size)
        val gain = sanitize(percent)
        var offset = 0
        while (offset + 1 < count) {
            val sample = ((buffer[offset].toInt() and 0xff) or (buffer[offset + 1].toInt() shl 8)).toShort().toInt()
            val amplified = (sample * gain / 100).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[offset] = amplified.toByte()
            buffer[offset + 1] = (amplified ushr 8).toByte()
            offset += 2
        }
    }
}
