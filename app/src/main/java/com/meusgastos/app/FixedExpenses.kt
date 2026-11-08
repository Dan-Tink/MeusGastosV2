@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------------------
// Aba "Fixas": as despesas que se repetem todo mês. Elas ficam sempre aqui; o botão "Pagar"
// lança o gasto de verdade (com valor e dia confirmados) e marca a fixa como paga no mês.
// ---------------------------------------------------------------------------------------

fun LazyListScope.fixedExpensesTab(
    month: YearMonth,
    items: List<FixedExpenseView>,
    onNew: () -> Unit,
    onPay: (FixedExpense) -> Unit,
    onEdit: (FixedExpense) -> Unit
) {
    val total = items.sumOf { it.fixed.amountCents }
    // Quando já foi pago, vale o que saiu de fato (a conta pode ter variado).
    val paidTotal = items.sumOf { it.paid?.amountCents ?: 0L }
    val dueTotal = items.filter { it.paid == null }.sumOf { it.fixed.amountCents }

    item(key = "fx-summary") { FixedSummaryCard(month, total, paidTotal, dueTotal, items.count { it.paid != null }, items.size) }

    item(key = "fx-new") {
        OutlinedButton(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Nova despesa fixa")
        }
    }

    if (items.isEmpty()) {
        item(key = "fx-empty") {
            Text(
                "Nenhuma despesa fixa neste mês. Cadastre aluguel, internet, mensalidades… e use \"Pagar\" todo mês.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
    items(items, key = { "fx-${it.fixed.id}" }) { v ->
        FixedRow(v, onPay = { onPay(v.fixed) }, onEdit = { onEdit(v.fixed) })
    }
}

@Composable
private fun FixedSummaryCard(month: YearMonth, total: Long, paid: Long, due: Long, paidCount: Int, count: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "FIXAS DE ${month.label().uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                total.toMoney(),
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            if (count > 0) {
                LinearProgressIndicator(
                    progress = { if (count == 0) 0f else paidCount.toFloat() / count },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    gapSize = 0.dp
                )
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Pago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(paid.toMoney(), fontFamily = MoneyFont, fontWeight = FontWeight.SemiBold, color = PositiveColor)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Falta pagar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(due.toMoney(), fontFamily = MoneyFont, fontWeight = FontWeight.SemiBold, color = WarningColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedRow(v: FixedExpenseView, onPay: () -> Unit, onEdit: () -> Unit) {
    val f = v.fixed
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(14.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(f.category, size = 44.dp, iconKey = f.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.description, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Vence dia ${f.dayOfMonth} · ${f.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (v.paid?.amountCents ?: f.amountCents).toMoney(),
                    fontFamily = MoneyFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                if (v.paid != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PositiveColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Pago ${LocalDate.ofEpochDay(v.paid.epochDay).dayOfMonth}/${"%02d".format(LocalDate.ofEpochDay(v.paid.epochDay).monthValue)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PositiveColor
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onPay,
                        shape = RoundedCornerShape(50),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Pagar") }
                }
            }
        }
    }
}

/** Confirma o pagamento do mês: valor e dia já vêm preenchidos; mude se a conta variou. */
@Composable
fun PayFixedSheet(
    fixed: FixedExpense,
    month: YearMonth,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultDay =
        if (month == YearMonth.now()) LocalDate.now().dayOfMonth
        else minOf(fixed.dayOfMonth.coerceAtLeast(1), month.lengthOfMonth())
    var amount by remember { mutableStateOf(fixed.amountCents.toPlain()) }
    var dayText by remember { mutableStateOf(defaultDay.toString()) }
    val cents = parseCents(amount)
    val day = dayText.toIntOrNull()
    val valid = cents != null && day != null && day in 1..month.lengthOfMonth()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor(),
        shape = SheetShape
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
                "Pagar ${fixed.description}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AmountHero(value = amount, onValueChange = { amount = it })
            TextField(
                value = dayText,
                onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.fillMaxWidth().focusGlow(14.dp),
                label = { Text("Dia do pagamento em ${month.label()}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = softFieldColors()
            )
            Text(
                "Vira um gasto de ${month.label()} (${fixed.type} · ${fixed.category}).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val d = month.atDay(day ?: 1)
                    onConfirm(cents ?: 0L, d.toEpochDay())
                    onDismiss()
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Confirmar pagamento", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Cria ou edita uma despesa fixa (o modelo que aparece todo mês na aba). */
@Composable
fun FixedExpenseSheet(
    editing: FixedExpense?,
    month: YearMonth,
    types: List<ExpenseType>,
    categories: List<Category>,
    lastType: String,
    lastCategory: String,
    onSave: (FixedExpense) -> Unit,
    onEnd: (FixedExpense) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var desc by remember { mutableStateOf(editing?.description ?: "") }
    var amount by remember { mutableStateOf(editing?.amountCents?.toPlain() ?: "") }
    var dayText by remember { mutableStateOf((editing?.dayOfMonth ?: 1).toString()) }
    var type by remember {
        mutableStateOf(
            editing?.type
                ?: types.firstOrNull { it.fixed }?.name
                ?: lastType.takeIf { t -> types.any { it.name == t } }
                ?: types.firstOrNull()?.name
                ?: ""
        )
    }
    var category by remember {
        mutableStateOf(editing?.category ?: lastCategory.takeIf { c -> categories.any { it.name == c } } ?: "Outros")
    }
    var icon by remember { mutableStateOf(editing?.icon) }
    var confirmEnd by remember { mutableStateOf(false) }

    val cents = parseCents(amount)
    val day = dayText.toIntOrNull()
    val valid = desc.isNotBlank() && cents != null && day != null && day in 1..31 && type.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor(),
        shape = SheetShape
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
                if (editing == null) "Nova despesa fixa" else "Editar despesa fixa",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AmountHero(value = amount, onValueChange = { amount = it })
            TextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth().focusGlow(14.dp),
                label = { Text("Descrição") },
                placeholder = { Text("ex.: Aluguel") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = softFieldColors()
            )
            TextField(
                value = dayText,
                onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.fillMaxWidth().focusGlow(14.dp),
                label = { Text("Dia do vencimento") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = softFieldColors()
            )

            FixedLabel("Tipo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t -> PillChip(type == t.name, { type = t.name }, t.name) }
            }

            FixedLabel("Categoria")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c -> PillChip(category == c.name, { category = c.name }, c.name) }
            }

            FixedLabel("Ícone (opcional)")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseIcons.forEach { (key, vector) ->
                    FixedIconChoice(icon == key, vector, key) { icon = if (icon == key) null else key }
                }
            }

            Text(
                if (editing == null) "Aparece na aba a partir de ${month.label()}. Não entra nos gastos até você tocar em Pagar."
                else "A mudança vale para os meses ainda não pagos. Gastos já pagos não mudam.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onSave(
                        FixedExpense(
                            id = editing?.id ?: 0L,
                            type = type,
                            category = category,
                            description = desc.trim(),
                            amountCents = cents ?: 0L,
                            dayOfMonth = day ?: 1,
                            startMonth = editing?.startMonth ?: month.toString(),
                            endMonth = editing?.endMonth,
                            icon = icon
                        )
                    )
                    onDismiss()
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (editing == null) "Adicionar despesa fixa" else "Salvar alteração", fontWeight = FontWeight.Bold) }

            if (editing != null) {
                TextButton(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Parar a partir de ${month.label()}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmEnd && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("Parar despesa fixa?") },
            text = {
                Text(
                    "\"${editing.description}\" deixa de aparecer a partir de ${month.label()}. " +
                        "Os meses anteriores e os gastos já pagos continuam como estão."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    onEnd(editing)
                    onDismiss()
                }) { Text("Parar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FixedLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FixedIconChoice(selected: Boolean, vector: ImageVector, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.primary else scheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            vector,
            contentDescription = description,
            tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
