package com.pixelveil.app

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Builds the repeating 32x32 (x cell) tile. Opaque black = pixel off, transparent = pixel lit. */
object PatternEngine {
    const val N = 32

    private val noiseRank: IntArray by lazy {
        val list = MutableList(N * N) { it }
        list.shuffle(java.util.Random(7L))
        IntArray(N * N) { list[it] }
    }

    /** 16x16 Bayer matrix value in 0..1 (dispersed dots, any percentage looks even). */
    private fun bayer(x: Int, y: Int): Float {
        val a = (x xor y) and 15
        val yy = y and 15
        var v = 0
        for (i in 0 until 4) {
            v = (v shl 2) or (((a shr i) and 1) shl 1) or ((yy shr i) and 1)
        }
        return (v + 0.5f) / 256f
    }

    /** k evenly spaced "lit" slots out of N. */
    private fun evenSet(k: Int): BooleanArray {
        val s = BooleanArray(N)
        for (j in 0 until k) {
            s[(((j + 0.5f) * N / k).toInt()).coerceIn(0, N - 1)] = true
        }
        return s
    }

    fun buildTile(p: PatternType, coveragePercent: Int, cell: Int, strengthPercent: Int): Bitmap {
        val cov = coveragePercent / 100f
        val f = 1f - cov                               // fraction of pixels that stay lit
        val lineSet = evenSet((f * N).roundToInt().coerceIn(1, N - 1))
        val gridSet = evenSet((sqrt(f) * N).roundToInt().coerceIn(1, N - 1))
        val size = N * cell
        val argb = Color.argb((255 * strengthPercent / 100).coerceIn(0, 255), 0, 0, 0)
        val px = IntArray(size * size)
        for (y in 0 until size) {
            val ty = (y / cell) % N
            for (x in 0 until size) {
                val tx = (x / cell) % N
                val off = when (p) {
                    PatternType.DITHER -> bayer(tx, ty) < cov
                    PatternType.NOISE -> (noiseRank[ty * N + tx] + 0.5f) / (N * N) < cov
                    PatternType.H_LINES -> !lineSet[ty]
                    PatternType.V_LINES -> !lineSet[tx]
                    PatternType.DIAG_A -> !lineSet[(tx + ty) and 31]
                    PatternType.DIAG_B -> !lineSet[(tx - ty) and 31]
                    PatternType.GRID -> !(gridSet[tx] && gridSet[ty])
                }
                if (off) px[y * size + x] = argb
            }
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        bmp.density = Bitmap.DENSITY_NONE
        return bmp
    }
}
