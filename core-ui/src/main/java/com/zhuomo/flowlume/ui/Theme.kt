package com.zhuomo.flowlume.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 全局设计 Tokens（对应 docs/02-ui/design-system.md） */
object FlowColors {
    val BgPrimary = Color(0xFF0A0A0F)
    val BgSecondary = Color(0xFF14141C)
    val BgCard = Color(0xFF12121A)
    val StrokeCard = Color(0x66FFFFFF)      // 白 40%
    val StrokeDivider = Color(0x1FFFFFFF)   // 白 12%
    val Accent = Color(0xFF9D7BFF)          // 淡紫主色
    val AccentPress = Color(0xFF7E5BEF)
    val TextPrimary = Color(0xFFF5F5FA)
    val TextSecondary = Color(0xFFA6A6B8)
    val TextTertiary = Color(0xFF6E6E80)
    val Success = Color(0xFF4ADE80)
    val Warning = Color(0xFFFBBF24)
    val Danger = Color(0xFFF87171)
    val Scrim = Color(0x99000000)
}

private val FlowScheme = darkColorScheme(
    primary = FlowColors.Accent,
    onPrimary = Color.White,
    secondary = FlowColors.AccentPress,
    background = FlowColors.BgPrimary,
    onBackground = FlowColors.TextPrimary,
    surface = FlowColors.BgCard,
    onSurface = FlowColors.TextPrimary,
    surfaceVariant = FlowColors.BgSecondary,
    onSurfaceVariant = FlowColors.TextSecondary,
    error = FlowColors.Danger
)

private val FlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val FlowTypography = Typography(
    // 标题：粗体无衬线英文
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp
    ),
    // 参数标签：全大写英文（调用方负责 toUpperCase）
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, letterSpacing = 0.8.sp
    ),
    // 正文中文
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp
    ),
    // 等宽数字（计时器、滑块数值）
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
)

@Composable
fun FlowLumeTheme(charcoal: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (charcoal) FlowScheme.copy(background = FlowColors.BgSecondary) else FlowScheme
    MaterialTheme(
        colorScheme = scheme,
        shapes = FlowShapes,
        typography = FlowTypography,
        content = content
    )
}
