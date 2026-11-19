package com.pixelveil.app

import android.content.Context

enum class PatternType(val label: String) {
    DITHER("Even dots"),
    GRID("Grid dots"),
    H_LINES("H lines"),
    V_LINES("V lines"),
    DIAG_A("Diagonal /"),
    DIAG_B("Diagonal \\"),
    NOISE("Noise")
}

enum class ShapeType(val label: String) {
    FULL("Full screen"),
    CIRCLE("Circle"),
    OVAL("Oval"),
    RECT("Rectangle"),
    ROUNDED("Rounded"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    HEXAGON("Hexagon"),
    STAR("Star"),
    HEART("Heart"),
    CROSS("Cross"),
    FRAME("Frame")
}

data class MaskSettings(
    val enabled: Boolean = false,
    val pattern: PatternType = PatternType.DITHER,
    val coverage: Int = 90,          // % of pixels turned off (5..95)
    val cell: Int = 1,               // size of one "pixel" of the pattern, in screen pixels (1..4)
    val shape: ShapeType = ShapeType.FULL,
    val invert: Boolean = false,     // true = pixels off OUTSIDE the shape
    val cx: Int = 50,                // shape centre X, % of screen width
    val cy: Int = 50,                // shape centre Y, % of screen height
    val sw: Int = 60,                // shape width, % of screen width
    val sh: Int = 40,                // shape height, % of screen height
    val strength: Int = 100,         // darkness of the "off" pixels (20..100)
    val useAccessibility: Boolean = false
)

object Prefs {
    private const val FILE = "pixelveil"

    private inline fun <reified T : Enum<T>> parse(name: String?, def: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: def

    fun load(ctx: Context): MaskSettings {
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val d = MaskSettings()
        return MaskSettings(
            enabled = p.getBoolean("enabled", d.enabled),
            pattern = parse(p.getString("pattern", null), d.pattern),
            coverage = p.getInt("coverage", d.coverage).coerceIn(5, 95),
            cell = p.getInt("cell", d.cell).coerceIn(1, 4),
            shape = parse(p.getString("shape", null), d.shape),
            invert = p.getBoolean("invert", d.invert),
            cx = p.getInt("cx", d.cx).coerceIn(0, 100),
            cy = p.getInt("cy", d.cy).coerceIn(0, 100),
            sw = p.getInt("sw", d.sw).coerceIn(5, 100),
            sh = p.getInt("sh", d.sh).coerceIn(5, 100),
            strength = p.getInt("strength", d.strength).coerceIn(20, 100),
            useAccessibility = p.getBoolean("acc", d.useAccessibility)
        )
    }

    fun save(ctx: Context, s: MaskSettings) {
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", s.enabled)
            .putString("pattern", s.pattern.name)
            .putInt("coverage", s.coverage)
            .putInt("cell", s.cell)
            .putString("shape", s.shape.name)
            .putBoolean("invert", s.invert)
            .putInt("cx", s.cx)
            .putInt("cy", s.cy)
            .putInt("sw", s.sw)
            .putInt("sh", s.sh)
            .putInt("strength", s.strength)
            .putBoolean("acc", s.useAccessibility)
            .apply()
    }
}
