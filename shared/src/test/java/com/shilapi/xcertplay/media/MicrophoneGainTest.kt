package com.shilapi.xcertplay.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ported from xcertplay acbef114. */
class MicrophoneGainTest {
    @Test fun gainRangeIsClamped() {
        assertEquals(80, MicrophoneGain.sanitize(20))
        assertEquals(130, MicrophoneGain.sanitize(130))
        assertEquals(200, MicrophoneGain.sanitize(500))
    }

    @Test fun settingsOfferEveryStepWithTheDefault() {
        assertEquals((80..200 step 10).toList(), MicrophoneGain.percents)
        assertEquals(true, MicrophoneGain.DEFAULT_PERCENT in MicrophoneGain.percents)
    }

    @Test fun pcmGainAmplifiesAndSaturatesInPlace() {
        val pcm = pcm(-20_000, -1_000, 0, 1_000, 20_000)
        MicrophoneGain.applyPcm16InPlace(pcm, pcm.size, 200)
        assertArrayEquals(pcm(-32_768, -2_000, 0, 2_000, 32_767), pcm)
    }

    @Test fun minimumGainAttenuatesSamples() {
        val pcm = pcm(-10_000, 10_000)
        MicrophoneGain.applyPcm16InPlace(pcm, pcm.size, 80)
        assertArrayEquals(pcm(-8_000, 8_000), pcm)
    }

    @Test fun onlyTheReadBytesChange() {
        val pcm = pcm(1_000, 1_000)
        MicrophoneGain.applyPcm16InPlace(pcm, 2, 200)
        assertArrayEquals(pcm(2_000, 1_000), pcm)
    }

    private fun pcm(vararg samples: Int): ByteArray = ByteArray(samples.size * 2).also { bytes ->
        samples.forEachIndexed { index, value ->
            bytes[index * 2] = value.toByte()
            bytes[index * 2 + 1] = (value ushr 8).toByte()
        }
    }
}
