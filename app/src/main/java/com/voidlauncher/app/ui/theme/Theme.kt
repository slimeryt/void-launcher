package com.voidlauncher.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.voidlauncher.app.R

val VoidCyan = Color(0xFF5EEAD4)
val VoidCyanDim = Color(0xFF2DD4BF)
val VoidGlass = Color(0x33FFFFFF)
val VoidGlassStrong = Color(0x55FFFFFF)
val VoidGlassBorder = Color(0x66FFFFFF)
val VoidInk = Color(0xFF05060A)
val VoidMist = Color(0xE6F4F7FA)
val VoidMuted = Color(0xB3D1D5DB)
/** iOS system blue for Done-style glass pills. */
val IosBlue = Color(0xFF0A84FF)
val IosBlueGlass = Color(0x990A84FF)

private val VoidColorScheme = darkColorScheme(
    primary = VoidCyan,
    onPrimary = VoidInk,
    secondary = VoidCyanDim,
    background = Color.Transparent,
    surface = VoidGlass,
    onSurface = VoidMist,
    onSurfaceVariant = VoidMuted
)

val VoidDisplay = FontFamily(
    Font(R.font.sf_compact_rounded_bold, FontWeight.Bold),
    Font(R.font.sf_compact_rounded_semibold, FontWeight.SemiBold)
)

val VoidBody = FontFamily(
    Font(R.font.sf_compact_rounded_regular, FontWeight.Normal),
    Font(R.font.sf_compact_rounded_medium, FontWeight.Medium),
    Font(R.font.sf_compact_rounded_semibold, FontWeight.SemiBold),
    Font(R.font.sf_compact_rounded_bold, FontWeight.Bold)
)

@Composable
fun VoidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidColorScheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = VoidDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 72.sp,
                letterSpacing = (-2).sp,
                color = VoidMist
            ),
            headlineMedium = TextStyle(
                fontFamily = VoidDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = VoidMist
            ),
            titleMedium = TextStyle(
                fontFamily = VoidBody,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = VoidMist
            ),
            labelLarge = TextStyle(
                fontFamily = VoidBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = VoidMist
            ),
            bodyLarge = TextStyle(
                fontFamily = VoidBody,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = VoidMist
            ),
            bodyMedium = TextStyle(
                fontFamily = VoidBody,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = VoidMuted
            ),
            labelSmall = TextStyle(
                fontFamily = VoidBody,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = VoidMuted
            )
        ),
        content = content
    )
}
