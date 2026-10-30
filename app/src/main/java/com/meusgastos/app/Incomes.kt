@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * Aba Ganhos.
 *  - Fixos: valem todo mês, sozinhos, até você parar.
 *  - Variáveis: valem só no mês em que foram lançados.
 */
fun LazyListScope.incomesTab(
    month: YearMonth,
    fixed: List<FixedIncome>,
    variable: List<Income>,
    onEditFixed: (FixedIncome) -> Unit,
    onEditVariable: (Income) -> Unit
) {
    val fixedTotal = fixed.sumOf { it.amountCents }
    val variableTotal = variable.sumOf { it.amountCents }

    item(key = "inc-total") { IncomeTotalCard(month, fixedTotal, variableTotal) }

    item(key = "inc-fixed-header") {
        SectionHeader("Fixos", "Entram sozinhos todo mês", fixedTotal)
    }
    if (fixed.isEmpty()) {
        item(key = "inc-fixed-empty") { EmptyHint("Nenhum ganho fixo. Ex.: salário, aluguel recebido.") }
    }
    items(fixed, key = { "inc-fixed-${it.id}" }) { f ->
        IncomeRow(
            icon = Icons.Default.Repeat,
            title = f.description,
            subtitle = "Todo dia ${f.dayOfMonth}",
            cents = f.amountCents,
            onClick = { onEditFixed(f) }
        )
    }

    item(key = "inc-var-header") {
        SectionHeader("Variáveis", "Contam só em ${month.label()}", variableTotal)
    }
    if (variable.isEmpty()) {
        item(key = "inc-var-empty") { EmptyHint("Nenhum ganho variável neste mês. Ex.: freela, bônus, venda.") }
    }
    items(variable, key = { "inc-var-${it.id}" }) { i ->
        IncomeRow(
            icon = Icons.Default.CalendarMonth,
            title = i.description,
            subtitle = LocalDate.ofEpochDay(i.epochDay).fmt(),
            cents = i.amountCents,
            onClick = { onEditVariable(i) }
        )
    }
}

@Composable
private fun IncomeTotalCard(month: YearMonth, fixedTotal: Long, variableTotal: Long) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "GANHOS DE ${month.label().uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                (fixedTotal + variableTotal).toMoney(),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold,
                color = PositiveColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Fixos ${fixedTotal.toMoney()}  ·  Variáveis ${variableTotal.toMoney()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, caption: String, total: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            total.toMoney(),
            fontFamily = MoneyFont,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun IncomeRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cents: Long,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PositiveColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PositiveColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                cents.toMoney(),
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold,
                color = PositiveColor
            )
        }
    }
}

/**
 * Folha para criar ou editar um ganho. Ao criar, você escolhe entre fixo (todo mês) e variável
 * (só um mês). Ao editar um fixo, a mudança vale do mês exibido em diante.
 */
@Composable
fun IncomeSheet(
    month: YearMonth,
    editingFixed: FixedIncome?,
    editingVariable: Income?,
    onSaveVariable: (Income) -> Unit,
    onDeleteVariable: (Income) -> Unit,
    onSaveFixed: (String, Long, Int, FixedIncome?) -> Unit,
    onEndFixed: (FixedIncome) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isNew = editingFixed == null && editingVariable == null
    val defaultDate = if (YearMonth.now() == month) LocalDate.now() else month.atDay(1)

    var fixedMode by remember { mutableStateOf(editingVariable == null) }
    var desc by remember {
        mutableStateOf(editingFixed?.description ?: editingVariable?.description ?: "")
    }
    var amount by remember {
        mutableStateOf((editingFixed?.amountCents ?: editingVariable?.amountCents)?.toPlain() ?: "")
    }
    var dayText by remember { mutableStateOf((editingFixed?.dayOfMonth ?: 1).toString()) }
    var date by remember {
        mutableStateOf(editingVariable?.let { LocalDate.ofEpochDay(it.epochDay) } ?: defaultDate)
    }
    var pickDate by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }

    val cents = parseCents(amount)
    val day = dayText.toIntOrNull()
    val valid = desc.isNotBlank() && cents != null && (!fixedMode || (day != null && day in 1..31))

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
                when {
                    isNew -> "Novo ganho"
                    editingFixed != null -> "Editar ganho fixo"
                    else -> "Editar ganho variável"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isNew) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(fixedMode, { fixedMode = true }, "Fixo · todo mês")
                    PillChip(!fixedMode, { fixedMode = false }, "Variável · só um mês")
                }
            }
            Text(
                if (fixedMode) {
                    "Entra sozinho em todos os meses a partir de ${month.label()}."
                } else {
                    "Conta só no mês da data escolhida."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição") },
                placeholder = { Text(if (fixedMode) "ex.: Salário" else "ex.: Freela") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = softFieldColors()
            )
            TextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor") },
                placeholder = { Text("1800,00") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = softFieldColors()
            )
            if (fixedMode) {
                TextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dia do mês em que cai") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = softFieldColors()
                )
            } else {
                androidx.compose.material3.Surface(
                    onClick = { pickDate = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(date.fmt(), fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (editingFixed != null) {
                Text(
                    "A mudança vale de ${month.label()} em diante. Os meses anteriores não mudam.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (fixedMode) {
                        onSaveFixed(desc.trim(), cents ?: 0L, day ?: 1, editingFixed)
                    } else {
                        onSaveVariable(
                            Income(
                                id = editingVariable?.id ?: 0L,
                                description = desc.trim(),
                                amountCents = cents ?: 0L,
                                epochDay = date.toEpochDay()
                            )
                        )
                    }
                    onDismiss()
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    when {
                        !isNew -> "Salvar alteração"
                        fixedMode -> "Adicionar ganho fixo"
                        else -> "Adicionar ganho variável"
                    }
                )
            }

            if (editingFixed != null) {
                TextButton(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Parar a partir de ${month.label()}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (editingVariable != null) {
                TextButton(
                    onClick = {
                        onDeleteVariable(editingVariable)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Excluir ganho", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }

    if (confirmEnd && editingFixed != null) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("Parar este ganho?") },
            text = {
                Text(
                    "\"${editingFixed.description}\" deixa de contar a partir de ${month.label()}. " +
                        "Os meses anteriores continuam como estão."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    onEndFixed(editingFixed)
                    onDismiss()
                }) { Text("Parar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancelar") } }
        )
    }
}
