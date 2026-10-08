package com.shilapi.xcertplay.media

import android.graphics.SurfaceTexture
import android.view.Surface
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Ported from xcertplay cda5386e: an iPhone can start sending frames before their codec config (seen with HEVC). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class AndroidMediaSinkMissingConfigTest {
    private val idr = byteArrayOf(0, 0, 0, 1, 0x65, 0x88.toByte(), 0x84.toByte(), 0x21)

    @Test fun framesWithoutACodecConfigAskTheIphoneForAKeyframe() {
        val sink = AndroidMediaSink(surface = null)
        try {
            val requested = CountDownLatch(1)
            sink.setVideoRecoveryHandler(110) { requested.countDown() }
            sink.setSurface(110, Surface(SurfaceTexture(0)))
            sink.onVideoFrame(110, idr)
            assertTrue("a keyframe was requested", requested.await(5, TimeUnit.SECONDS))
        } finally {
            sink.close()
            sink.awaitVideoReleased(5_000)
        }
    }
}
