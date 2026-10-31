package com.meusgastos.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
    background = Color(0xFFF4F6F4),
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
    background = Color(0xFF0A100D),
    onBackground = Color(0xFFE3EBE5),
    surface = Color(0xFF18241E),
    onSurface = Color(0xFFE3EBE5),
    surfaceVariant = Color(0xFF1E2D25),
    onSurfaceVariant = Color(0xFF9DB0A3),
    surfaceTint = Color(0xFF4CD07D),
    outline = Color(0xFF5A7364),
    outlineVariant = Color(0xFF2F473A),
    error = Color(0xFFF2878F),
    onError = Color(0xFF3A0A10),
    errorContainer = Color(0xFF5A1D24),
    onErrorContainer = Color(0xFFFFDAD9),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF101914),
    surfaceContainer = Color(0xFF16211B),
    surfaceContainerHigh = Color(0xFF1B2822),
    surfaceContainerHighest = Color(0xFF2A3D33)
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
    headerBackground = Color(0xFF18A649),
    onHeader = Color.White,
    onHeaderMuted = Color.White
)

private val DarkAppColors = AppColors(
    positive = Color(0xFF5ADB8F),
    warning = Color(0xFFE6B95C),
    violet = Color(0xFFB4A6F7),
    headerBackground = Color(0xFF1A7F43),
    onHeader = Color.White,
    onHeaderMuted = Color(0xFFE3F3E8)
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

/** Inter (licença SIL OFL), em quatro pesos. Números tabulares: os valores se alinham em coluna. */
val AppFont: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/** Fonte dos valores em dinheiro. */
val MoneyFont: FontFamily get() = AppFont

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

private fun TextStyle.app(): TextStyle = copy(fontFamily = AppFont, fontFeatureSettings = "tnum")

// Inter em tudo, com números tabulares. Títulos mais firmes e rótulos pequenos com respiro.
private val AppTypography = Typography(
    displayLarge = Base.displayLarge.app(),
    displayMedium = Base.displayMedium.app(),
    displaySmall = Base.displaySmall.app(),
    headlineLarge = Base.headlineLarge.app(),
    headlineMedium = Base.headlineMedium.app(),
    headlineSmall = Base.headlineSmall.app(),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold).app(),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold).app(),
    titleSmall = Base.titleSmall.app(),
    bodyLarge = Base.bodyLarge.app(),
    bodyMedium = Base.bodyMedium.app(),
    bodySmall = Base.bodySmall.app(),
    labelLarge = Base.labelLarge.app(),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp).app(),
    labelSmall = Base.labelSmall.copy(letterSpacing = 0.4.sp).app()
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

// ---------------------------------------------------------------------------------------
// Cores de destaque. Cada uma muda só a "tinta" do app (botões, cabeçalho, seleção e o leve tom
// dos fundos); a estrutura, os tamanhos e o significado das cores (verde = ganho, vermelho =
// dívida/limite) continuam iguais. Todos os botões mantêm texto com contraste de 4,5:1 ou mais.
// ---------------------------------------------------------------------------------------

private class AccentSpec(
    val swatch: Color,
    // claro
    val lPrimary: Color, val lContainer: Color, val lOnContainer: Color, val lHeader: Color,
    val lBackground: Color, val lHighest: Color, val lOutline: Color,
    // escuro
    val dPrimary: Color, val dOnPrimary: Color, val dContainer: Color, val dOnContainer: Color,
    val dHeader: Color, val dBackground: Color, val dSurface: Color, val dVariant: Color,
    val dOutline: Color, val dHighest: Color
)

enum class AccentTheme(val key: String, val label: String) {
    VERDE("verde", "Verde"),
    AZUL("azul", "Azul"),
    ROXO("roxo", "Roxo"),
    ROSA("rosa", "Rosa"),
    VERMELHO("vermelho", "Vermelho"),
    LARANJA("laranja", "Laranja");

    /** Cor mostrada no seletor (a do cabeçalho no tema claro). */
    val swatch: Color get() = spec().swatch

    companion object {
        fun from(key: String?): AccentTheme = entries.firstOrNull { it.key == key } ?: VERDE
    }
}

private fun c(v: Long) = Color(v)

private fun AccentTheme.spec(): AccentSpec = when (this) {
    AccentTheme.VERDE -> AccentSpec(
        c(0xFF18A649),
        c(0xFF12873C), c(0xFFD8F3E1), c(0xFF083D1B), c(0xFF18A649), c(0xFFF4F6F4), c(0xFFEAF0EC), c(0xFFDDE5DF),
        c(0xFF4CD07D), c(0xFF05260F), c(0xFF14452A), c(0xFFC6F2D6), c(0xFF1A7F43),
        c(0xFF0A100D), c(0xFF18241E), c(0xFF1E2D25), c(0xFF2F473A), c(0xFF2A3D33)
    )
    AccentTheme.AZUL -> AccentSpec(
        c(0xFF1976D2),
        c(0xFF1565C0), c(0xFFD6E6FA), c(0xFF0B2F5E), c(0xFF1976D2), c(0xFFF3F6FA), c(0xFFE7EDF4), c(0xFFDAE2EC),
        c(0xFF7DB6F5), c(0xFF06223F), c(0xFF123B66), c(0xFFD3E6FB), c(0xFF1B5FA8),
        c(0xFF090E14), c(0xFF141D28), c(0xFF1A2635), c(0xFF2B4259), c(0xFF25374B)
    )
    AccentTheme.ROXO -> AccentSpec(
        c(0xFF7B4FD6),
        c(0xFF6A3FC8), c(0xFFE7DDFA), c(0xFF2A1466), c(0xFF7B4FD6), c(0xFFF6F4FA), c(0xFFEEEAF5), c(0xFFE1DCEC),
        c(0xFFB8A2F2), c(0xFF24104F), c(0xFF3B2677), c(0xFFE6DDFB), c(0xFF5B37A8),
        c(0xFF0E0B14), c(0xFF1B1626), c(0xFF241D33), c(0xFF3D3357), c(0xFF322A47)
    )
    AccentTheme.ROSA -> AccentSpec(
        c(0xFFD81B6F),
        c(0xFFC2185B), c(0xFFFBDDE9), c(0xFF5B0A2D), c(0xFFD81B6F), c(0xFFFAF4F6), c(0xFFF5E9EE), c(0xFFEBDCE3),
        c(0xFFF48FB8), c(0xFF4A0A28), c(0xFF6B1B41), c(0xFFFFD9E8), c(0xFFA8195A),
        c(0xFF140B0F), c(0xFF261720), c(0xFF301D28), c(0xFF4E3040), c(0xFF3F2634)
    )
    AccentTheme.VERMELHO -> AccentSpec(
        c(0xFFD32F2F),
        c(0xFFC62828), c(0xFFFADDDA), c(0xFF5C0A0A), c(0xFFD32F2F), c(0xFFFAF4F3), c(0xFFF5E8E6), c(0xFFEBDAD7),
        c(0xFFF2827E), c(0xFF4D0907), c(0xFF6E1C1A), c(0xFFFFDAD7), c(0xFFA52A27),
        c(0xFF140C0B), c(0xFF261918), c(0xFF301F1D), c(0xFF4E3633), c(0xFF3F2B29)
    )
    AccentTheme.LARANJA -> AccentSpec(
        c(0xFFCF5A08),
        c(0xFFB84E06), c(0xFFFDE3CF), c(0xFF5A2400), c(0xFFCF5A08), c(0xFFFAF6F2), c(0xFFF5ECE4), c(0xFFEBDFD4),
        c(0xFFF7A261), c(0xFF4A2100), c(0xFF6B3410), c(0xFFFFDFC6), c(0xFFA8500D),
        c(0xFF140E09), c(0xFF26190F), c(0xFF30211A), c(0xFF4E3A2C), c(0xFF3F2E22)
    )
}

private fun lightFor(a: AccentTheme): ColorScheme {
    val s = a.spec()
    return LightScheme.copy(
        primary = s.lPrimary,
        primaryContainer = s.lContainer,
        onPrimaryContainer = s.lOnContainer,
        secondaryContainer = s.lContainer,
        onSecondaryContainer = s.lOnContainer,
        surfaceTint = s.lPrimary,
        background = s.lBackground,
        surfaceContainerHighest = s.lHighest,
        outlineVariant = s.lOutline
    )
}

private fun darkFor(a: AccentTheme): ColorScheme {
    val s = a.spec()
    return DarkScheme.copy(
        primary = s.dPrimary,
        onPrimary = s.dOnPrimary,
        primaryContainer = s.dContainer,
        onPrimaryContainer = s.dOnContainer,
        secondaryContainer = s.dContainer,
        onSecondaryContainer = s.dOnContainer,
        surfaceTint = s.dPrimary,
        background = s.dBackground,
        surface = s.dSurface,
        surfaceVariant = s.dVariant,
        outlineVariant = s.dOutline,
        surfaceContainerHighest = s.dHighest
    )
}

private fun appColorsFor(a: AccentTheme, dark: Boolean): AppColors {
    val s = a.spec()
    val base = if (dark) DarkAppColors else LightAppColors
    return AppColors(
        positive = base.positive,
        warning = base.warning,
        violet = base.violet,
        headerBackground = if (dark) s.dHeader else s.lHeader,
        onHeader = base.onHeader,
        onHeaderMuted = base.onHeaderMuted
    )
}

@Composable
fun MeusGastosTheme(
    darkTheme: Boolean,
    accent: AccentTheme = AccentTheme.VERDE,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) darkFor(accent) else lightFor(accent)
    val app = appColorsFor(accent, darkTheme)

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
