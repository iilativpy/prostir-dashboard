package com.prostir.dashboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The line icons of the design prototype (24x24 grid, 2 px round stroke). */
enum class Glyph(val path: String) {
    Home("M3 11l9-8 9 8M5 10v10h14V10"),
    Trend("M3 17l6-6 4 4 8-8M15 7h6v6"),
    Grid("M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"),
    Swap("M4 8h13l-3-3M20 16H7l3 3"),
    Phone("M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"),
    ChevronRight("M9 6l6 6-6 6"),
    ChevronLeft("M15 6l-6 6 6 6"),
    Refresh("M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5"),
    WifiOff("M3 3l18 18M8.5 16.4a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 4-2.5M12 20h.01M19 12.9a10 10 0 0 0-5-2.7"),
    Alert("M12 8v5M12 17h.01M10.3 3.9L2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"),
    Lock("M6 11V8a6 6 0 1 1 12 0v3M5 11h14v10H5z"),
    Calendar("M4 6h16v14H4zM4 10h16M8 3v4M16 3v4"),
    CheckIn("M4 12h12M12 7l5 5-5 5M20 4v16"),
    CheckOut("M20 12H8M12 7l-5 5 5 5M4 4v16"),
    Dots("M12 5h.01M12 12h.01M12 19h.01"),
}

@Composable
fun ProIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, contentDescription: String? = null) {
    val path = remember(glyph) { PathParser().parsePathString(glyph.path).toPath() }
    val semantic = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Canvas(modifier.size(size).then(semantic)) {
        scale(scaleX = this.size.width / 24f, scaleY = this.size.height / 24f, pivot = Offset.Zero) {
            drawPath(path, tint, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
