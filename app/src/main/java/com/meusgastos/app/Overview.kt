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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
private val PendingColor = Color(0xFFFB923C)

@Composable
fun OverviewCard(
    o: Overview,
    limit: Long?,
    prevLabel: String,
    onIncomeClick: () -> Unit
) {
    val spent = o.spent
    val balanceColor = if (o.balance < 0) MaterialTheme.colorScheme.error else GreenColor
    val afterPendingColor = if (o.afterPending < 0) MaterialTheme.colorScheme.error else GreenColor

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
                Tile("Pendente", o.pending, PendingColor, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Tile("Saldo", o.balance, balanceColor, Modifier.weight(1f))
                Tile("Após pendentes", o.afterPending, afterPendingColor, Modifier.weight(1f))
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

/** Linha de um ganho na aba Resumo. Ganho fixo mostra o selo "Fixo". */
@Composable
fun IncomeRow(income: Income, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        income.description,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (income.recurringId != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Fixo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    LocalDate.ofEpochDay(income.epochDay).fmt(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(income.amountCents.toMoney(), fontWeight = FontWeight.Bold, color = GreenColor)
        }
    }
}

private enum class IncomeAction { SAVE, DELETE }

/**
 * Criar ou editar um ganho.
 * Novo: "Fixo (todo mês)" cria um ganho que se repete sozinho; "Só este mês" vale apenas no mês.
 * Editar um ganho fixo pergunta se vale só para este mês ou também para os próximos.
 */
@Composable
fun IncomeSheet(
    initial: Income?,
    month: YearMonth,
    onSaveOnce: (Income) -> Unit,
    onSaveFixed: (String, Long, LocalDate) -> Unit,
    onSaveScoped: (Income, GroupScope) -> Unit,
    onDeleteScoped: (Income, GroupScope) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultDate = if (YearMonth.now() == month) LocalDate.now() else month.atDay(1)
    var fixed by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.amountCents?.toPlain() ?: "") }
    var date by remember {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.epochDay) } ?: defaultDate)
    }
    var pickDate by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<IncomeAction?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isFixedExisting = initial?.recurringId != null
    val cents = parseCents(amount)
    val valid = desc.isNotBlank() && cents != null

    fun buildIncome() = Income(
        id = initial?.id ?: 0L,
        description = desc.trim(),
        amountCents = cents ?: 0L,
        epochDay = date.toEpochDay(),
        recurringId = initial?.recurringId,
        ym = initial?.ym
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "Novo ganho" else "Editar ganho",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (initial == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !fixed,
                        onClick = { fixed = false },
                        label = { Text("Só este mês") }
                    )
                    FilterChip(
                        selected = fixed,
                        onClick = { fixed = true },
                        label = { Text("Fixo (todo mês)") }
                    )
                }
                Text(
                    if (fixed) {
                        "Ex.: salário. Entra sozinho todo mês, no mesmo dia, a partir do mês da data escolhida."
                    } else {
                        "Ex.: freela, presente. Vale só para o mês da data escolhida."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isFixedExisting) {
                Text(
                    "Ganho fixo: se repete todo mês. Ao salvar, você escolhe se vale só para este mês ou também para os próximos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

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
                Text(
                    if (initial == null && fixed) "Dia do recebimento: ${date.fmt()}"
                    else "Data: ${date.fmt()}"
                )
            }

            Button(
                onClick = {
                    when {
                        initial == null && fixed -> onSaveFixed(desc.trim(), cents ?: 0L, date)
                        initial == null -> onSaveOnce(buildIncome())
                        isFixedExisting -> {
                            action = IncomeAction.SAVE
                        }
                        else -> onSaveOnce(buildIncome())
                    }
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (initial == null && fixed) "Criar ganho fixo" else "Salvar") }

            if (initial != null) {
                TextButton(
                    onClick = {
                        if (isFixedExisting) {
                            action = IncomeAction.DELETE
                        } else {
                            confirmDelete = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Excluir ganho", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir ganho?") },
            text = { Text("\"${initial.description}\" será removido deste mês.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteScoped(initial, GroupScope.ONE)
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }

    val pending = action
    if (pending != null && initial != null) {
        val deleting = pending == IncomeAction.DELETE
        val color = if (deleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = { action = null },
            title = { Text(if (deleting) "Excluir ganho fixo" else "Salvar alterações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (deleting) "\"${initial.description}\" se repete todo mês. O que excluir?"
                        else "\"${initial.description}\" se repete todo mês. Onde aplicar a mudança?"
                    )
                    TextButton(
                        onClick = {
                            action = null
                            if (deleting) onDeleteScoped(initial, GroupScope.ONE)
                            else onSaveScoped(buildIncome(), GroupScope.ONE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Só este mês", color = color) }
                    TextButton(
                        onClick = {
                            action = null
                            if (deleting) onDeleteScoped(initial, GroupScope.FROM_HERE)
                            else onSaveScoped(buildIncome(), GroupScope.FROM_HERE)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (deleting) "Este e os próximos (encerra o fixo)" else "Este e os próximos meses",
                            color = color
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } }
        )
    }
}
