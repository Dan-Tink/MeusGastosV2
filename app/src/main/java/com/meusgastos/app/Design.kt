package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------
// Peças visuais compartilhadas: ícone de categoria, chip leve (sem borda) e campo "suave".
// ---------------------------------------------------------------------------------------

/** Ícone de cada categoria. Categorias criadas pelo usuário recebem um ícone genérico. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "Alimentação" -> Icons.Default.Restaurant
    "Transporte" -> Icons.Default.DirectionsCar
    "Moradia" -> Icons.Default.Home
    "Saúde" -> Icons.Default.LocalHospital
    "Lazer" -> Icons.Default.SportsEsports
    "Compras" -> Icons.Default.ShoppingBag
    "Contas" -> Icons.Default.ReceiptLong
    "Outros" -> Icons.Default.MoreHoriz
    else -> Icons.Default.Category
}

/** Círculo com o ícone da categoria, na cor dela (fundo suave, ícone escuro/claro conforme o tema). */
@Composable
fun CategoryBadge(name: String, size: Dp = 44.dp, selected: Boolean = false) {
    val tint = categoryColor(name)
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) tint else tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            categoryIcon(name),
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Chip sem borda: fundo suave quando não selecionado e verde cheio com texto branco quando
 * selecionado (contraste do verde primário com branco é de 4,5:1 ou mais).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillChip(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/** Campos preenchidos, com fundo suave e sem linha embaixo. */
@Composable
fun softFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = MaterialTheme.colorScheme.error
)

/** Valor em reais com os centavos menores ("R$ 1.820" grande e ",00" pequeno). */
fun moneyAnnotated(cents: Long, centsScale: Float = 0.6f): AnnotatedString {
    val t = cents.toMoney()
    val i = t.lastIndexOf(',')
    return buildAnnotatedString {
        if (i < 0) {
            append(t)
        } else {
            append(t.substring(0, i))
            withStyle(SpanStyle(fontSize = centsScale.em)) { append(t.substring(i)) }
        }
    }
}
