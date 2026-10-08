package com.shilapi.xcertplay

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlAnswerTest {
    @Test fun theFirstAnswerIsKept() {
        val answer = UrlAnswer()
        answer.complete(mapOf("FCUP_Response_StatusCode" to 200))
        answer.complete(mapOf("FCUP_Response_StatusCode" to 404))

        assertEquals(mapOf("FCUP_Response_StatusCode" to 200), answer.get(1, TimeUnit.SECONDS))
    }

    @Test fun anEmptyAnswerEndsTheWait() {
        val answer = UrlAnswer()
        answer.complete(null)

        assertNull(answer.get(1, TimeUnit.SECONDS))
    }

    @Test(expected = TimeoutException::class)
    fun noAnswerTimesOut() {
        UrlAnswer().get(10, TimeUnit.MILLISECONDS)
    }
}
