package com.meusgastos.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta pensada para uso prolongado à noite: fundo azul-petróleo bem escuro (não preto puro),
// texto off-white (não branco puro) e acentos dessaturados, para não "vibrar" sobre o fundo.
// Todos os pares texto/fundo ficam acima de 6:1 de contraste (WCAG AA exige 4.5:1).
private val Bg = Color(0xFF0E131D)
private val SurfaceColor = Color(0xFF141B28)
private val CardColor = Color(0xFF1A2333)
private val Teal = Color(0xFF5FC4D6)
private val Violet = Color(0xFFAE9CF5)

/** Verde dos ganhos/saldo positivo. */
val PositiveColor = Color(0xFF5FD3A5)

/** Amarelo de atenção (gastos fixos, limite perto de estourar). */
val WarningColor = Color(0xFFE6B95C)

/** Violeta dos gastos variáveis. */
val AccentViolet = Violet

/** Fonte dos valores em dinheiro: monoespaçada, dá o ar de painel técnico e alinha os dígitos. */
val MoneyFont: FontFamily = FontFamily.Monospace

/** Borda fina e discreta dos cartões, no lugar de sombras pesadas. */
val CardBorder = BorderStroke(1.dp, Color(0xFF273248))

private val Scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF062229),
    secondary = Violet,
    onSecondary = Color(0xFF1C1540),
    background = Bg,
    onBackground = Color(0xFFDCE3EE),
    surface = SurfaceColor,
    onSurface = Color(0xFFDCE3EE),
    surfaceVariant = CardColor,
    onSurfaceVariant = Color(0xFF93A1BA),
    outline = Color(0xFF273248),
    outlineVariant = Color(0xFF1F2940),
    // Containers definidos à mão: sem isso o Material usa o roxo padrão nos chips, menus e diálogos.
    primaryContainer = Color(0xFF16424C),
    onPrimaryContainer = Color(0xFFC3EEF6),
    secondaryContainer = Color(0xFF232E47),
    onSecondaryContainer = Color(0xFFDCD6FA),
    surfaceContainerLowest = Color(0xFF0B1019),
    surfaceContainerLow = Color(0xFF111824),
    surfaceContainer = Color(0xFF161E2C),
    surfaceContainerHigh = Color(0xFF1A2333),
    surfaceContainerHighest = Color(0xFF202B3E),
    error = Color(0xFFF08A93),
    onError = Color(0xFF3A0A10)
)

// Cantos um pouco mais retos: visual mais técnico. Diálogos e bottom sheets usam o "extraLarge".
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val Base = Typography()

// Rótulos pequenos com um pouco de espaçamento entre letras: leitura mais limpa e "técnica".
private val AppTypography = Typography(
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    labelSmall = Base.labelSmall.copy(letterSpacing = 0.6.sp)
)

private val TypePalette = listOf(
    Color(0xFF5FC4D6),
    Color(0xFFAE9CF5),
    Color(0xFF5FD3A5),
    Color(0xFFE6B95C),
    Color(0xFFF08FBF),
    Color(0xFF7FADF0),
    Color(0xFFF0A070)
)

fun typeColor(name: String): Color =
    TypePalette[(name.hashCode() and Int.MAX_VALUE) % TypePalette.size]

fun paletteColor(index: Int): Color = TypePalette[index % TypePalette.size]

private val CategoryColors = mapOf(
    "Alimentação" to Color(0xFFF0A070),
    "Transporte" to Color(0xFF7FADF0),
    "Moradia" to Color(0xFFAE9CF5),
    "Saúde" to Color(0xFFF08FBF),
    "Lazer" to Color(0xFF5FD3A5),
    "Compras" to Color(0xFFE6B95C),
    "Contas" to Color(0xFF5FC4D6),
    "Outros" to Color(0xFF8FA0B8)
)

fun categoryColor(name: String): Color = CategoryColors[name] ?: typeColor(name)

@Composable
fun MeusGastosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
