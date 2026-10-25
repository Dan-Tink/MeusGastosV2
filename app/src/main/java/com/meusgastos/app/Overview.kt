@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private val GreenColor = Color(0xFF34D399)
private val AmberColor = Color(0xFFFBBF24)
private val VioletColor = Color(0xFFA78BFA)

@Composable
fun OverviewCard(
    o: Overview,
    limit: Long?,
    prevLabel: String,
    onIncomeClick: () -> Unit
) {
    val spent = o.spent
    val over = o.balance < 0
    val balanceColor = if (over) MaterialTheme.colorScheme.error else GreenColor

    // Referência da barra: o limite definido ou, na falta dele, os ganhos do mês.
    val reference: Long? = limit ?: o.income.takeIf { it > 0 }
    val pct: Long = if (reference != null && reference > 0) spent * 100 / reference else 0
    val ratio = if (reference != null && reference > 0) {
        (spent.toFloat() / reference.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val barColor = when {
        limit != null && pct >= 100 -> MaterialTheme.colorScheme.error
        limit != null && pct >= 80 -> AmberColor
        limit == null && spent > o.income -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Tile("Ganhos", o.income, GreenColor, Modifier.weight(1f), onClick = onIncomeClick)
                Tile("Gastos fixos", o.fixed, AmberColor, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Tile("Gastos variáveis", o.variable, VioletColor, Modifier.weight(1f))
                Tile("Saldo", o.balance, balanceColor, Modifier.weight(1f))
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
                    diff > 0 -> "Você gastou $diff% a mais que em $prevLabel$period" to AmberColor
                    diff < 0 -> "Você gastou ${-diff}% a menos que em $prevLabel$period" to GreenColor
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
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar ganhos",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            cents.toMoney(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IncomesSheet(
    month: YearMonth,
    incomes: List<Income>,
    onSave: (Income) -> Unit,
    onDelete: (Income) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultDate = if (YearMonth.now() == month) LocalDate.now() else month.atDay(1)

    var editing by remember { mutableStateOf<Income?>(null) }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(defaultDate) }
    var pickDate by remember { mutableStateOf(false) }

    val cents = parseCents(amount)
    val valid = desc.isNotBlank() && cents != null

    fun reset() {
        editing = null
        desc = ""
        amount = ""
        date = defaultDate
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Ganhos de ${month.label()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Total: ${incomes.sumOf { it.amountCents }.toMoney()}",
                color = GreenColor,
                fontWeight = FontWeight.SemiBold
            )

            if (incomes.isEmpty()) {
                Text(
                    "Nenhum ganho lançado neste mês.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            incomes.forEach { inc ->
                Card(
                    onClick = {
                        editing = inc
                        desc = inc.description
                        amount = inc.amountCents.toPlain()
                        date = LocalDate.ofEpochDay(inc.epochDay)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                inc.description,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                LocalDate.ofEpochDay(inc.epochDay).fmt(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            inc.amountCents.toMoney(),
                            fontWeight = FontWeight.Bold,
                            color = GreenColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                if (editing == null) "Novo ganho" else "Editar ganho",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição") },
                placeholder = { Text("ex.: Salário") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor") },
                placeholder = { Text("1800,00") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedButton(
                onClick = { pickDate = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(date.fmt())
            }
            Button(
                onClick = {
                    onSave(
                        Income(
                            id = editing?.id ?: 0L,
                            description = desc.trim(),
                            amountCents = cents ?: 0L,
                            epochDay = date.toEpochDay()
                        )
                    )
                    reset()
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (editing == null) "Adicionar ganho" else "Salvar alteração") }

            if (editing != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { reset() }) { Text("Cancelar edição") }
                    TextButton(onClick = {
                        editing?.let { onDelete(it) }
                        reset()
                    }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }
}
