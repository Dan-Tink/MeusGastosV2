package com.meusgastos.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Design Tokens: medidas e formas num lugar só. Cores continuam no tema (Theme.kt), que já
 * troca sozinho entre as seis cores de destaque e entre claro e escuro.
 */

/** Espaçamentos. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    /** Respiro interno dos cartões de destaque. */
    val xxl: Dp = 24.dp
}

/** Raios de canto. */
object Radii {
    val field: Dp = 14.dp
    val card: Dp = 20.dp
    val sheet: Dp = 24.dp
}

/** Barras de progresso: finas e totalmente arredondadas. */
object Bars {
    val height: Dp = 6.dp
    val shape = RoundedCornerShape(50)
}

/** Bottom sheets: topo com 24 dp de raio. */
val SheetShape = RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet)

/** Fundo dos sheets: no escuro, um tom acima do fundo do app; no claro, o branco de sempre. */
@Composable
fun sheetColor(): Color {
    val s = MaterialTheme.colorScheme
    return if (s.surface.luminance() < 0.5f) s.surfaceContainer else s.surface
}

/**
 * Campo com borda discreta que, ao receber foco, passa para a cor do tema e ganha um leve brilho.
 * Usa a cor primária do tema ativo, então vale para as seis cores.
 */
fun Modifier.focusGlow(radius: Dp = Radii.field): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val borderColor by animateColorAsState(if (focused) primary else idle, label = "focusBorder")
    val glow by animateFloatAsState(if (focused) 0.20f else 0f, label = "focusGlow")
    this
        .onFocusChanged { focused = it.hasFocus }
        .drawBehind {
            if (glow > 0f) {
                val grow = 4.dp.toPx()
                drawRoundRect(
                    color = primary.copy(alpha = glow),
                    topLeft = Offset(-grow, -grow),
                    size = Size(size.width + 2 * grow, size.height + 2 * grow),
                    cornerRadius = CornerRadius(radius.toPx() + grow)
                )
            }
        }
        .border(1.dp, borderColor, RoundedCornerShape(radius))
}
