package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ChildCare
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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

/** Círculo com o ícone da categoria. No escuro o fundo é mais forte e o ícone assume a cor da categoria. */
@Composable
fun CategoryBadge(name: String, size: Dp = 44.dp, selected: Boolean = false, iconKey: String? = null) {
    val tint = if (name == "Outros") MaterialTheme.colorScheme.onSurfaceVariant else categoryColor(name)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bg = when {
        selected -> tint
        dark -> tint.copy(alpha = 0.15f)
        else -> tint.copy(alpha = 0.15f)
    }
    val fg = when {
        selected -> Color.White
        dark -> androidx.compose.ui.graphics.lerp(tint, Color.White, 0.55f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            (expenseIconFor(iconKey) ?: categoryIcon(name)),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Escolha entre poucas opções (Fixa/Variável etc.): uma "calha" em forma de pílula com a seleção
 * deslizando por cima dela, na cor do tema.
 */
@Composable
fun SegmentedChoice(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val n = options.size.coerceAtLeast(1)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(4.dp)
    ) {
        val segment = maxWidth / n
        val thumbX by animateDpAsState(segment * selectedIndex, label = "segmentThumb")
        Box(
            Modifier
                .offset(x = thumbX)
                .width(segment)
                .height(44.dp)
                .clip(CircleShape)
                .background(scheme.primary)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val active = i == selectedIndex
                val textColor by animateColorAsState(
                    if (active) scheme.onPrimary else scheme.onSurface, label = "segmentText"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
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
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
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

/**
 * Valor em destaque: "R$" ao lado e o cursor sempre no início do campo, sobre o "0,00" de
 * referência (o campo centralizado do Material deixava o cursor no meio do placeholder).
 */
@Composable
fun AmountHero(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier
) {
    val style: TextStyle = MaterialTheme.typography.displaySmall.copy(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // "R$" menor e mais leve, apoiado na linha de base do número: o valor é o protagonista.
        Text(
            "R$",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
        )
        // O texto mostrado é sempre a versão com máscara; o cursor fica sempre no fim.
        val shown = maskMoney(value)
        var field by remember { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
        if (field.text != shown) field = TextFieldValue(shown, TextRange(shown.length))
        BasicTextField(
            value = field,
            onValueChange = { n ->
                val m = maskMoney(n.text)
                field = TextFieldValue(m, TextRange(m.length))
                onValueChange(m)
            },
            modifier = fieldModifier.width(IntrinsicSize.Min),
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (shown.isEmpty()) {
                        Text("0,00", style = style.copy(color = MaterialTheme.colorScheme.outline))
                    }
                    inner()
                }
            }
        )
    }
}

/** Ícones que o usuário pode escolher para um gasto (a chave é o que fica salvo). */
val ExpenseIcons: List<Pair<String, ImageVector>> = listOf(
    "cafe" to Icons.Default.LocalCafe,
    "comida" to Icons.Default.Restaurant,
    "mercado" to Icons.Default.LocalGroceryStore,
    "carro" to Icons.Default.DirectionsCar,
    "combustivel" to Icons.Default.LocalGasStation,
    "casa" to Icons.Default.Home,
    "luz" to Icons.Default.Bolt,
    "agua" to Icons.Default.WaterDrop,
    "internet" to Icons.Default.Wifi,
    "celular" to Icons.Default.PhoneAndroid,
    "contas" to Icons.Default.ReceiptLong,
    "saude" to Icons.Default.LocalHospital,
    "compras" to Icons.Default.ShoppingBag,
    "roupa" to Icons.Default.Checkroom,
    "jogos" to Icons.Default.SportsEsports,
    "cinema" to Icons.Default.Movie,
    "musica" to Icons.Default.MusicNote,
    "academia" to Icons.Default.FitnessCenter,
    "pet" to Icons.Default.Pets,
    "escola" to Icons.Default.School,
    "crianca" to Icons.Default.ChildCare,
    "viagem" to Icons.Default.FlightTakeoff,
    "presente" to Icons.Default.CardGiftcard,
    "ferramenta" to Icons.Default.Build,
    "trabalho" to Icons.Default.Work,
    "poupanca" to Icons.Default.Savings
)

fun expenseIconFor(key: String?): ImageVector? =
    if (key == null) null else ExpenseIcons.firstOrNull { it.first == key }?.second
