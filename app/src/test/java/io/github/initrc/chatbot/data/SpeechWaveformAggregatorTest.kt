package io.github.initrc.chatbot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechWaveformAggregatorTest {

    @Test
    fun normalizeSpeechAmplitudeClampsToZeroAndOne() {
        assertEquals(0f, normalizeSpeechAmplitude(-1), 0.0001f)
        assertEquals(0f, normalizeSpeechAmplitude(0), 0.0001f)
        assertEquals(0.5f, normalizeSpeechAmplitude(4_096), 0.0001f)
        assertEquals(1f, normalizeSpeechAmplitude(8_192), 0.0001f)
        assertEquals(1f, normalizeSpeechAmplitude(16_384), 0.0001f)
    }

    @Test
    fun addRawAmplitudeAveragesConfiguredSampleCountBeforeAppendingBar() {
        val aggregator = SpeechWaveformAggregator(barCount = 1, samplesPerBar = 4)

        assertNull(aggregator.addRawAmplitude(0))
        assertNull(aggregator.addRawAmplitude(2_048))
        assertNull(aggregator.addRawAmplitude(4_096))
        val bars = aggregator.addRawAmplitude(8_192)

        assertEquals(listOf(0.4375f), bars)
    }

    @Test
    fun addRawAmplitudeDropsOldestRealBarWhenWindowIsFull() {
        val aggregator = SpeechWaveformAggregator(barCount = 2, samplesPerBar = 1)

        aggregator.addRawAmplitude(4_096)
        aggregator.addRawAmplitude(8_192)
        val bars = aggregator.addRawAmplitude(2_048)

        assertEquals(listOf(1f, 0.25f), bars)
    }

    @Test
    fun resetClearsPendingSamplesAndBars() {
        val aggregator = SpeechWaveformAggregator(barCount = 2, samplesPerBar = 2)

        aggregator.addRawAmplitude(8_192)
        assertEquals(listOf(0f, 0f), aggregator.reset())
        val bars = aggregator.addRawAmplitude(4_096)

        assertNull(bars)
    }
}
