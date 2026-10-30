package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Painel do mês (aba Resumo): o saldo em destaque, de onde veio e para onde foi o dinheiro,
 * a barra do limite e a comparação com o mês anterior.
 */
@Composable
fun OverviewCard(
    o: Overview,
    limit: Long?,
    prevLabel: String,
    onIncomeClick: () -> Unit
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    val spent = o.spent
    val over = o.balance < 0
    val balanceColor = if (over) MaterialTheme.colorScheme.error else PositiveColor

    // Referência da barra: o limite definido ou, na falta dele, os ganhos do mês.
    val reference: Long? = limit ?: o.income.takeIf { it > 0 }
    val pct: Long = if (reference != null && reference > 0) spent * 100 / reference else 0
    val ratio = if (reference != null && reference > 0) {
        (spent.toFloat() / reference.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val barColor = when {
        limit != null && pct >= 100 -> MaterialTheme.colorScheme.error
        limit != null && pct >= 80 -> WarningColor
        limit == null && spent > o.income -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Saldo do mês",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (hidden) "R$ ••••••" else o.balance.toMoney(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = MoneyFont,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { hidden = !hidden }) {
                    Icon(
                        if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hidden) "Mostrar valores" else "Ocultar valores",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("Ganhos", o.income, PositiveColor, hidden, Modifier.weight(1f), onClick = onIncomeClick)
                Tile("Gastos fixos", o.fixed, WarningColor, hidden, Modifier.weight(1f))
                Tile("Variáveis", o.variable, AccentViolet, hidden, Modifier.weight(1f))
            }

            if (reference != null) {
                LinearProgressIndicator(
                    progress = { ratio },
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }

            val barText = when {
                limit != null -> {
                    val alert = when {
                        pct >= 100 -> " — limite estourado!"
                        pct >= 80 -> " — atenção!"
                        else -> ""
                    }
                    "$pct% do limite de ${limit.toMoney()}$alert"
                }
                o.income > 0 -> "$pct% dos ganhos já foram gastos"
                else -> "Toque em Ganhos para lançar seu salário e acompanhar o saldo"
            }
            Text(
                barText,
                style = MaterialTheme.typography.labelSmall,
                color = if (limit != null && pct >= 80) barColor
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (o.prevSpent > 0 && spent > 0) {
                val diff = (spent - o.prevSpent) * 100 / o.prevSpent
                val period = o.prevUpToDay?.let { " (até o dia $it)" } ?: ""
                val (text, color) = when {
                    diff > 0 -> "Você gastou $diff% a mais que em $prevLabel$period" to WarningColor
                    diff < 0 -> "Você gastou ${-diff}% a menos que em $prevLabel$period" to PositiveColor
                    else -> "Mesmo gasto de $prevLabel$period" to
                        MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

@Composable
private fun Tile(
    label: String,
    cents: Long,
    color: Color,
    hidden: Boolean,
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Column(base.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver ganhos",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (hidden) "••••" else cents.toMoney(),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = MoneyFont,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
