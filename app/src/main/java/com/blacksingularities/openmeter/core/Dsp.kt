package com.blacksingularities.openmeter.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** FFT radix-2 w miejscu; n musi być potęgą dwójki. */
class Fft(val n: Int) {
    private val cosT = DoubleArray(n / 2) { cos(2 * PI * it / n) }
    private val sinT = DoubleArray(n / 2) { -sin(2 * PI * it / n) }
    private val rev = IntArray(n).also { r ->
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) r[i] = Integer.reverse(i) ushr (32 - bits)
    }

    /** Okno Hanna; współczynnik amplitudy 0,5. */
    val hann = DoubleArray(n) { 0.5 - 0.5 * cos(2 * PI * it / (n - 1)) }

    fun transform(re: DoubleArray, im: DoubleArray) {
        for (i in 0 until n) {
            val j = rev[i]
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var i = 0
            while (i < n) {
                var k = 0
                for (j in i until i + half) {
                    val l = j + half
                    val tr = re[l] * cosT[k] - im[l] * sinT[k]
                    val ti = re[l] * sinT[k] + im[l] * cosT[k]
                    re[l] = re[j] - tr
                    im[l] = im[j] - ti
                    re[j] += tr
                    im[j] += ti
                    k += step
                }
                i += size
            }
            size *= 2
        }
    }

    /**
     * Widmo amplitudowe (n/2 binów) z oknem Hanna, po odjęciu składowej stałej.
     * Sinus o amplitudzie A daje w swoim binie wartość ≈ A.
     */
    fun magnitudes(samples: DoubleArray, out: DoubleArray) {
        var mean = 0.0
        for (v in samples) mean += v
        mean /= n
        val re = DoubleArray(n) { (samples[it] - mean) * hann[it] }
        val im = DoubleArray(n)
        transform(re, im)
        for (k in 0 until n / 2) out[k] = sqrt(re[k] * re[k] + im[k] * im[k]) * 4.0 / n
    }
}

/** Indeks binu z największą wartością (od [from]) i jego położenie dokładniejsze przez interpolację parabolą. */
fun peakBin(mag: DoubleArray, from: Int = 1, to: Int = mag.size - 1): Double {
    var best = from
    for (k in from until to) if (mag[k] > mag[best]) best = k
    if (best <= 0 || best >= mag.size - 1) return best.toDouble()
    val a = mag[best - 1]
    val b = mag[best]
    val c = mag[best + 1]
    val d = a - 2 * b + c
    return if (d == 0.0) best.toDouble() else best + 0.5 * (a - c) / d
}

/** Korekcja A (IEC 61672) w dB dla częstotliwości f. */
fun aWeightDb(f: Double): Double {
    if (f <= 0) return -200.0
    val f2 = f * f
    val ra = (12194.0 * 12194.0 * f2 * f2) /
        ((f2 + 20.6 * 20.6) * sqrt((f2 + 107.7 * 107.7) * (f2 + 737.9 * 737.9)) * (f2 + 12194.0 * 12194.0))
    return 20 * log10(ra) + 2.0
}

private val NOTES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "H")

/** Nazwa dźwięku i odchyłka w centach (A4 = 440 Hz). */
fun noteOf(freq: Double): Pair<String, Int>? {
    if (freq < 16 || freq > 20000) return null
    val midi = 69 + 12 * log2(freq / 440.0)
    val n = midi.roundToInt()
    val cents = ((midi - n) * 100).roundToInt()
    val name = NOTES[((n % 12) + 12) % 12] + (n / 12 - 1)
    return name to cents
}

fun Double.db(): Double = if (this <= 0) -200.0 else 20 * log10(this)
