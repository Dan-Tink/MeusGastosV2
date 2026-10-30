package com.meusgastos.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Como o app escolhe o tema: seguindo o celular ou fixo em claro/escuro. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

// ---------------------------------------------------------------------------------------
// Identidade visual: verde (como na referência), cartões arredondados, cabeçalho verde com
// cantos curvos e botão "+" redondo no centro da barra inferior.
// Duas paletas completas. Todos os pares texto/fundo usados passam de 4,5:1 de contraste
// (WCAG AA); bordas de campos de entrada e ícones passam de 3:1.
// ---------------------------------------------------------------------------------------

private val LightScheme = lightColorScheme(
    primary = Color(0xFF12873C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3E1),
    onPrimaryContainer = Color(0xFF083D1B),
    secondary = Color(0xFF5B4BC4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F3E1),
    onSecondaryContainer = Color(0xFF083D1B),
    tertiary = Color(0xFF0E7C86),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F2),
    onBackground = Color(0xFF0F1A13),
    surface = Color.White,
    onSurface = Color(0xFF0F1A13),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF55645B),
    surfaceTint = Color(0xFF12873C),
    outline = Color(0xFF7D8F83),
    outlineVariant = Color(0xFFDDE5DF),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAF8),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEAF0EC)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF4CD07D),
    onPrimary = Color(0xFF05260F),
    primaryContainer = Color(0xFF14452A),
    onPrimaryContainer = Color(0xFFC6F2D6),
    secondary = Color(0xFFB4A6F7),
    onSecondary = Color(0xFF1C1540),
    secondaryContainer = Color(0xFF1E4A31),
    onSecondaryContainer = Color(0xFFC6F2D6),
    tertiary = Color(0xFF6CCBD3),
    onTertiary = Color(0xFF00282B),
    background = Color(0xFF0D1411),
    onBackground = Color(0xFFE3EBE5),
    surface = Color(0xFF131C17),
    onSurface = Color(0xFFE3EBE5),
    surfaceVariant = Color(0xFF1A2620),
    onSurfaceVariant = Color(0xFF9DB0A3),
    surfaceTint = Color(0xFF4CD07D),
    outline = Color(0xFF5A7364),
    outlineVariant = Color(0xFF263A30),
    error = Color(0xFFF2878F),
    onError = Color(0xFF3A0A10),
    errorContainer = Color(0xFF5A1D24),
    onErrorContainer = Color(0xFFFFDAD9),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF101914),
    surfaceContainer = Color(0xFF16211B),
    surfaceContainerHigh = Color(0xFF1B2822),
    surfaceContainerHighest = Color(0xFF22312A)
)

/** Cores de significado (ganho, aviso, destaque) e do cabeçalho, que mudam com o tema. */
@Immutable
class AppColors(
    val positive: Color,
    val warning: Color,
    val violet: Color,
    val headerBackground: Color,
    val onHeader: Color,
    val onHeaderMuted: Color
)

private val LightAppColors = AppColors(
    positive = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    violet = Color(0xFF6D4FD6),
    headerBackground = Color(0xFF12873C),
    onHeader = Color.White,
    onHeaderMuted = Color.White
)

private val DarkAppColors = AppColors(
    positive = Color(0xFF5ADB8F),
    warning = Color(0xFFE6B95C),
    violet = Color(0xFFB4A6F7),
    headerBackground = Color(0xFF0F3A22),
    onHeader = Color(0xFFE9F5EC),
    onHeaderMuted = Color(0xFFB7D3C0)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/** Verde dos ganhos e do saldo positivo. */
val PositiveColor: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.positive

/** Âmbar de atenção (gastos fixos, limite perto de estourar). */
val WarningColor: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning

/** Violeta dos gastos variáveis. */
val AccentViolet: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.violet

/** Fundo do cabeçalho com o mês (verde nos dois temas). */
val HeaderBackground: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.headerBackground

val OnHeader: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.onHeader

val OnHeaderMuted: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.onHeaderMuted

/** Fonte dos valores em dinheiro. Sem fonte própria: usa a do sistema, como na referência. */
val MoneyFont: FontFamily = FontFamily.Default

/** Borda fina e discreta dos cartões, no lugar de sombras pesadas. */
val CardBorder: BorderStroke
    @Composable @ReadOnlyComposable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

// Cantos arredondados: cartões 16 dp, diálogos e bottom sheets 20 dp.
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val Base = Typography()

// Títulos mais firmes e rótulos pequenos com um pouco de respiro entre as letras.
private val AppTypography = Typography(
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
    labelSmall = Base.labelSmall.copy(letterSpacing = 0.4.sp)
)

// Cores de gráficos e marcadores: tons médios que passam de 3:1 tanto no fundo branco quanto
// nos cartões escuros, então a mesma paleta serve nos dois temas.
private val TypePalette = listOf(
    Color(0xFF0E9AA7),
    Color(0xFF7C5CE0),
    Color(0xFF1FA85A),
    Color(0xFFC47400),
    Color(0xFFD6458F),
    Color(0xFF3B82F6),
    Color(0xFFE8590C)
)

fun typeColor(name: String): Color =
    TypePalette[(name.hashCode() and Int.MAX_VALUE) % TypePalette.size]

fun paletteColor(index: Int): Color = TypePalette[index % TypePalette.size]

private val CategoryColors = mapOf(
    "Alimentação" to Color(0xFFE8590C),
    "Transporte" to Color(0xFF3B82F6),
    "Moradia" to Color(0xFF7C5CE0),
    "Saúde" to Color(0xFFD6458F),
    "Lazer" to Color(0xFF1FA85A),
    "Compras" to Color(0xFFC47400),
    "Contas" to Color(0xFF0E9AA7),
    "Outros" to Color(0xFF7A8C80)
)

fun categoryColor(name: String): Color = CategoryColors[name] ?: typeColor(name)

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun MeusGastosTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val app = if (darkTheme) DarkAppColors else LightAppColors

    // Barras do sistema acompanham o tema: a de cima fica na cor do cabeçalho (ícones claros) e a de
    // baixo na cor da barra de navegação do app.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            window.statusBarColor = app.headerBackground.toArgb()
            window.navigationBarColor = scheme.surface.toArgb()
            window.setBackgroundDrawable(ColorDrawable(scheme.background.toArgb()))
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides app) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
