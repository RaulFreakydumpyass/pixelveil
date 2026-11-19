package com.pixelveil.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

private val Accent = Color(0xFF5CE1B0)
private val CardBg = Color(0xFF141416)
private val Muted = Color(0xFF9A9AA0)
private val Tile = Color(0xFF1C1C1F)

class MainActivity : ComponentActivity() {
    private var state by mutableStateOf(MaskSettings())
    private var tick by mutableIntStateOf(0)
    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        state = Prefs.load(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    onPrimary = Color.Black,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = CardBg,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF2A2A2E),
                    onSurfaceVariant = Muted
                )
            ) {
                Surface(color = Color.Black, contentColor = Color.White) {
                    Screen(
                        s = state,
                        tick = tick,
                        onChange = { change(it) },
                        onToggle = { toggle(it) },
                        onOverlayPerm = { openOverlayPermission() },
                        onAccessibility = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        state = Prefs.load(this)
        tick++
        MaskController.sync(this)
    }

    private fun change(n: MaskSettings) {
        state = n
        Prefs.save(this, n)
        MaskController.sync(this)
    }

    private fun toggle(on: Boolean) {
        if (on) {
            val accReady = state.useAccessibility && PixelVeilAccessibility.instance != null
            if (!accReady && !Settings.canDrawOverlays(this)) {
                openOverlayPermission()
                return
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        change(state.copy(enabled = on))
    }

    private fun openOverlayPermission() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }
}

private class Preset(val name: String, val make: (MaskSettings) -> MaskSettings)

private val presets = listOf(
    Preset("Ultra dark 90%") {
        it.copy(pattern = PatternType.DITHER, coverage = 90, cell = 1, shape = ShapeType.FULL, invert = false, strength = 100)
    },
    Preset("Saver 50%") {
        it.copy(pattern = PatternType.DITHER, coverage = 50, cell = 1, shape = ShapeType.FULL, invert = false)
    },
    Preset("Night lines 75%") {
        it.copy(pattern = PatternType.H_LINES, coverage = 75, cell = 1, shape = ShapeType.FULL, invert = false)
    },
    Preset("Spotlight") {
        it.copy(pattern = PatternType.DITHER, coverage = 90, cell = 1, shape = ShapeType.CIRCLE, invert = true, cx = 50, cy = 50, sw = 80, sh = 80)
    },
    Preset("Top half off") {
        it.copy(pattern = PatternType.GRID, coverage = 90, shape = ShapeType.RECT, invert = false, cx = 50, cy = 25, sw = 100, sh = 50)
    },
    Preset("Heart") {
        it.copy(pattern = PatternType.DIAG_A, coverage = 90, shape = ShapeType.HEART, invert = false, cx = 50, cy = 50, sw = 80, sh = 40)
    }
)

private class Area(val name: String, val cx: Int, val cy: Int, val sw: Int, val sh: Int)

private val areas = listOf(
    Area("Top half", 50, 25, 100, 50),
    Area("Bottom half", 50, 75, 100, 50),
    Area("Left half", 25, 50, 50, 100),
    Area("Right half", 75, 50, 50, 100),
    Area("Status bar", 50, 2, 100, 4),
    Area("Nav bar", 50, 98, 100, 4)
)

@Composable
private fun Screen(
    s: MaskSettings,
    tick: Int,
    onChange: (MaskSettings) -> Unit,
    onToggle: (Boolean) -> Unit,
    onOverlayPerm: () -> Unit,
    onAccessibility: () -> Unit
) {
    val ctx = LocalContext.current
    val hasOverlay = remember(tick) { Settings.canDrawOverlays(ctx) }
    val accOn = remember(tick) { PixelVeilAccessibility.instance != null }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("PixelVeil", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Power card
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Pixels off", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    if (s.enabled) "Active · ${s.coverage}% · ${s.pattern.label}" else "Off",
                    fontSize = 13.sp, color = if (s.enabled) Accent else Muted
                )
            }
            Switch(checked = s.enabled, onCheckedChange = onToggle)
        }

        Section("Live preview") {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .height(325.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, Color(0xFF333338), RoundedCornerShape(22.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77), Color(0xFF4D96FF))
                            )
                        )
                )
                AndroidView(
                    factory = { c -> PixelMaskView(c) },
                    update = { it.setMask(s) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text("Shown at real pixel size, so the dots match what your screen will do.", fontSize = 12.sp, color = Muted)
        }

        Section("Quick presets") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p -> Chip(p.name, false) { onChange(p.make(s)) } }
            }
        }

        Section("Pattern") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PatternType.values().forEach { p ->
                    PatternCard(p, s.coverage, s.pattern == p) { onChange(s.copy(pattern = p)) }
                }
            }
            LabeledSlider("Pixels turned off", s.coverage, 5..95, "%") { onChange(s.copy(coverage = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 50, 75, 90).forEach { v ->
                    Chip("$v%", s.coverage == v) { onChange(s.copy(coverage = v)) }
                }
            }
            LabeledSlider("Pattern pixel size", s.cell, 1..4, " px", steps = 2) { onChange(s.copy(cell = it)) }
            LabeledSlider("Off-pixel darkness", s.strength, 20..100, "%") { onChange(s.copy(strength = it)) }
        }

        Section("Shape") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShapeType.values().forEach { sh ->
                    ShapeCard(sh, s.shape == sh) { onChange(s.copy(shape = sh)) }
                }
            }
            if (s.shape != ShapeType.FULL) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Turn off pixels OUTSIDE the shape", fontSize = 14.sp, color = Color.White)
                    Switch(checked = s.invert, onCheckedChange = { onChange(s.copy(invert = it)) })
                }
                LabeledSlider("Center X", s.cx, 0..100, "%") { onChange(s.copy(cx = it)) }
                LabeledSlider("Center Y", s.cy, 0..100, "%") { onChange(s.copy(cy = it)) }
                LabeledSlider("Width", s.sw, 5..100, "%") { onChange(s.copy(sw = it)) }
                LabeledSlider("Height", s.sh, 5..100, "%") { onChange(s.copy(sh = it)) }
            }
            Text("Quick areas", fontSize = 13.sp, color = Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                areas.forEach { a ->
                    Chip(a.name, false) {
                        onChange(s.copy(shape = ShapeType.RECT, invert = false, cx = a.cx, cy = a.cy, sw = a.sw, sh = a.sh))
                    }
                }
            }
        }

        Section("How it is drawn") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Overlay", !s.useAccessibility) { onChange(s.copy(useAccessibility = false)) }
                Chip("Accessibility", s.useAccessibility) { onChange(s.copy(useAccessibility = true)) }
            }
            if (!s.useAccessibility) {
                Text(
                    "Overlay: needs \"Display over other apps\". Android 12+ caps overlay opacity at 80% so touches keep working, so off-pixels reach about 80% black.",
                    fontSize = 13.sp, color = Muted
                )
                if (!hasOverlay) Button(onClick = onOverlayPerm) { Text("Allow display over other apps") }
            } else {
                Text(
                    "Accessibility: true black pixels, and it also covers the status bar and navigation bar. It only draws the overlay and never reads your screen.",
                    fontSize = 13.sp, color = Muted
                )
                if (!accOn) {
                    Button(onClick = onAccessibility) { Text("Open accessibility settings") }
                    Text(
                        "Installed from an APK? If the switch is greyed out: Settings > Apps > PixelVeil > ⋮ (top right) > Allow restricted settings, then try again. Until it is on, Overlay mode is used.",
                        fontSize = 12.sp, color = Muted
                    )
                } else {
                    Text("Accessibility service is on ✓", fontSize = 13.sp, color = Accent)
                }
            }
        }

        Text(
            "Black pixels use almost no power on AMOLED screens like the S23 FE. High percentages make the screen very dim and hard to read. There is also a \"Pixels off\" tile you can add to Quick Settings.",
            fontSize = 12.sp, color = Muted, modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        content()
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Color(0xFF1F4D3E) else Tile)
            .border(1.dp, if (selected) Accent else Color(0xFF333338), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) Accent else Color.White, maxLines = 1)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    steps: Int = 0,
    onValue: (Int) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = Color.White)
            Text("$value$suffix", fontSize = 14.sp, color = Accent)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps
        )
    }
}

@Composable
private fun PatternCard(p: PatternType, coverage: Int, selected: Boolean, onClick: () -> Unit) {
    val bmp = remember(p, coverage) { PatternEngine.buildTile(p, coverage, 1, 100) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .width(88.dp)
            .clip(shape)
            .background(Tile)
            .border(2.dp, if (selected) Accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))) {
            drawRect(Color.White)
            drawImage(
                image = bmp.asImageBitmap(),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
        Text(p.label, fontSize = 11.sp, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun ShapeCard(sh: ShapeType, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .width(80.dp)
            .clip(shape)
            .background(Tile)
            .border(2.dp, if (selected) Accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(Modifier.size(48.dp)) {
            drawIntoCanvas { c ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
                val path = ShapeFactory.build(sh, size.width, size.height, 50, 50, 90, 90)
                if (path == null) {
                    c.nativeCanvas.drawRoundRect(2f, 2f, size.width - 2f, size.height - 2f, 8f, 8f, paint)
                } else {
                    c.nativeCanvas.drawPath(path, paint)
                }
            }
        }
        Text(sh.label, fontSize = 11.sp, color = Color.White, maxLines = 1)
    }
}
