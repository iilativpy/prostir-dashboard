package com.prostir.dashboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.prostir.dashboard.R

/** Design tokens of direction D "Будинки" (see design/spec-d.md). */
@Immutable
class ProstirColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val onAccent: Color,
    val good: Color,
    val goodSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val bad: Color,
    val badSoft: Color,
    val heat0: Color,
    val heat1: Color,
    val onHeat1: Color,
    /** Cell value from which the heatmap text switches to [onHeat1]. */
    val hotThreshold: Int,
    val houseText: Color,
    /** House 1..4. Always shown together with the house number. */
    val houses: List<Color>,
    /** Newest year first: thick solid, solid, dashed. */
    val years: List<Color>,
)

val LightColors = ProstirColors(
    isDark = false,
    bg = Color(0xFFEBF0F4), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFDFE7EE),
    ink = Color(0xFF13202C), muted = Color(0xFF566574), line = Color(0xFFD2DCE5),
    accent = Color(0xFF1D3A56), accentSoft = Color(0xFFDCE7F2), accentInk = Color(0xFF1D3A56), onAccent = Color.White,
    good = Color(0xFF18774F), goodSoft = Color(0xFFD6F0E3),
    warn = Color(0xFFA64B07), warnSoft = Color(0xFFFDE8CF),
    bad = Color(0xFFB42318), badSoft = Color(0xFFFBE0DD),
    heat0 = Color(0xFFE8EEF4), heat1 = Color(0xFF1D3A56), onHeat1 = Color.White, hotThreshold = 70,
    houseText = Color.White,
    houses = listOf(Color(0xFF2E8B63), Color(0xFF3A6FD8), Color(0xFFC96A0C), Color(0xFFA34A8C)),
    years = listOf(Color(0xFF4B50C8), Color(0xFF0A8DA8), Color(0xFFB7791F)),
)

val DarkColors = ProstirColors(
    isDark = true,
    bg = Color(0xFF0F161D), surface = Color(0xFF18222C), surface2 = Color(0xFF212E3A),
    ink = Color(0xFFE8EEF3), muted = Color(0xFF93A3B1), line = Color(0xFF2A3846),
    accent = Color(0xFF8DB8E8), accentSoft = Color(0xFF1F3347), accentInk = Color(0xFFB7D4F2), onAccent = Color(0xFF0F161D),
    good = Color(0xFF58CC96), goodSoft = Color(0xFF16342A),
    warn = Color(0xFFF0A45D), warnSoft = Color(0xFF3A2A16),
    bad = Color(0xFFFF8A80), badSoft = Color(0xFF3F1D1A),
    heat0 = Color(0xFF1B2733), heat1 = Color(0xFF6FA8E6), onHeat1 = Color(0xFF0E1A26), hotThreshold = 50,
    houseText = Color(0xFF0B1118),
    houses = listOf(Color(0xFF34A474), Color(0xFF5580E0), Color(0xFFD2771F), Color(0xFFB658B0)),
    years = listOf(Color(0xFF7C82EE), Color(0xFF22A5B6), Color(0xFFBE8522)),
)

private val LocalProstir = staticCompositionLocalOf { LightColors }

object Pro {
    val c: ProstirColors
        @Composable @ReadOnlyComposable get() = LocalProstir.current
}

@OptIn(ExperimentalTextApi::class)
val Onest = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            resId = R.font.onest,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

@Composable
fun ProstirTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.line, error = colors.bad,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.line, error = colors.bad,
        )
    }
    CompositionLocalProvider(LocalProstir provides colors) {
        MaterialTheme(colorScheme = scheme) {
            ProvideTextStyle(TextStyle(fontFamily = Onest, color = colors.ink, lineHeight = 1.3.em)) {
                content()
            }
        }
    }
}
