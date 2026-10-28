package com.meusgastos.app

import androidx.compose.foundation.clickable
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
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SALDO DO MÊS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    o.balance.toMoney(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = MoneyFont,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(Modifier.fillMaxWidth()) {
                Tile("Ganhos", o.income, PositiveColor, Modifier.weight(1f), onClick = onIncomeClick)
                Tile("Gastos fixos", o.fixed, WarningColor, Modifier.weight(1f))
                Tile("Variáveis", o.variable, AccentViolet, Modifier.weight(1f))
            }

            if (reference != null) {
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.outline
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
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Column(base.padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onClick != null) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver ganhos",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            cents.toMoney(),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = MoneyFont,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
