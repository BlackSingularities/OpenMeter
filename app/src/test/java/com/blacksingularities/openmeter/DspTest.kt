package com.blacksingularities.openmeter

import com.blacksingularities.openmeter.core.Fft
import com.blacksingularities.openmeter.core.aWeightDb
import com.blacksingularities.openmeter.core.noteOf
import com.blacksingularities.openmeter.core.peakBin
import com.blacksingularities.openmeter.update.isNewer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class DspTest {
    @Test
    fun fftFindsSineFrequencyAndAmplitude() {
        val n = 4096
        val sr = 44100.0
        val f = 1000.0
        val fft = Fft(n)
        val x = DoubleArray(n) { 0.5 * sin(2 * PI * f * it / sr) }
        val mag = DoubleArray(n / 2)
        fft.magnitudes(x, mag)
        val pk = peakBin(mag)
        assertEquals(f, pk * sr / n, 3.0)
        assertEquals(0.5, mag[pk.toInt()], 0.1)
    }

    @Test
    fun aWeightingReferencePoints() {
        assertEquals(0.0, aWeightDb(1000.0), 0.1)
        assertEquals(-19.1, aWeightDb(100.0), 0.3)
    }

    @Test
    fun notes() {
        assertEquals("A4", noteOf(440.0)!!.first)
        assertEquals("C4", noteOf(261.63)!!.first)
    }

    @Test
    fun versions() {
        assertTrue(isNewer("1.0.12", "1.0.9"))
        assertFalse(isNewer("1.0.0", "1.0.0"))
        assertFalse(isNewer("0.9", "1.0.0"))
    }
}
