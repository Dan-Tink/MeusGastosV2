@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------
// Tipos de despesa (fixo / variável)
// ---------------------------------------------------------------------------------------

@Composable
fun TypesDialog(
    types: List<ExpenseType>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onToggleFixed: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tipos de despesa") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(typeColor(t.name))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name)
                            Text(
                                if (t.fixed) "Fixo (toque p/ alternar)" else "Variável (toque p/ alternar)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onToggleFixed(t.name, !t.fixed) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { renaming = t.name; input = t.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear")
                        }
                        IconButton(onClick = { onDelete(t.name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (renaming != null) "Novo nome" else "Novo tipo") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    val r = renaming
                    if (r != null) onRename(r, input) else onAdd(input)
                    input = ""
                    renaming = null
                }
            ) { Text(if (renaming != null) "Salvar" else "Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ---------------------------------------------------------------------------------------
// Categorias (Alimentação, Transporte, Lazer...)
// ---------------------------------------------------------------------------------------

@Composable
fun CategoriesDialog(
    categories: List<Category>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categorias") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(categoryColor(c.name))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            c.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { renaming = c.name; input = c.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear")
                        }
                        IconButton(onClick = { onDelete(c.name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (renaming != null) "Novo nome" else "Nova categoria") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    val r = renaming
                    if (r != null) onRename(r, input) else onAdd(input)
                    input = ""
                    renaming = null
                }
            ) { Text(if (renaming != null) "Salvar" else "Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ---------------------------------------------------------------------------------------
// Gastos e ganhos recorrentes (aluguel, internet, salário...)
// ---------------------------------------------------------------------------------------

@Composable
fun RecurringSheet(
    recurring: List<Recurring>,
    types: List<ExpenseType>,
    categories: List<Category>,
    onSave: (Recurring) -> Unit,
    onDelete: (Recurring) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editing by remember { mutableStateOf<Recurring?>(null) }
    var isIncome by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(types.firstOrNull()?.name ?: "") }
    var category by remember { mutableStateOf("Outros") }
    var desc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf("1") }

    val cents = parseCents(amount)
    val day = dayText.toIntOrNull()
    val valid = desc.isNotBlank() && cents != null && day != null && day in 1..31 &&
        (isIncome || type.isNotBlank())

    fun reset() {
        editing = null
        isIncome = false
        type = types.firstOrNull()?.name ?: ""
        category = "Outros"
        desc = ""
        amount = ""
        dayText = "1"
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
                "Gastos recorrentes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Lançados sozinhos todo mês, no dia escolhido. Mudanças valem a partir do próximo mês.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recurring.isEmpty()) {
                Text(
                    "Nada cadastrado ainda. Ex.: aluguel, internet, salário.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            recurring.forEach { r ->
                Card(
                    onClick = {
                        editing = r
                        isIncome = r.isIncome
                        type = r.type.ifBlank { types.firstOrNull()?.name ?: "" }
                        category = r.category.ifBlank { "Outros" }
                        desc = r.description
                        amount = r.amountCents.toPlain()
                        dayText = r.dayOfMonth.toString()
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
                                r.description,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Todo dia ${r.dayOfMonth} • " +
                                    if (r.isIncome) "Ganho" else "${r.type} • ${r.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            r.amountCents.toMoney(),
                            fontWeight = FontWeight.Bold,
                            color = if (r.isIncome) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                if (editing == null) "Novo recorrente" else "Editar recorrente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    label = { Text("Gasto") }
                )
                FilterChip(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    label = { Text("Ganho") }
                )
            }

            if (!isIncome) {
                Text(
                    "Tipo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t.name,
                            onClick = { type = t.name },
                            label = { Text(t.name) }
                        )
                    }
                }
                Text(
                    "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c.name,
                            onClick = { category = c.name },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição") },
                placeholder = { Text(if (isIncome) "ex.: Salário" else "ex.: Aluguel") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Valor") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.width(110.dp),
                    label = { Text("Dia") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Button(
                onClick = {
                    onSave(
                        Recurring(
                            id = editing?.id ?: 0L,
                            isIncome = isIncome,
                            type = if (isIncome) "" else type,
                            category = if (isIncome) "" else category,
                            description = desc.trim(),
                            amountCents = cents ?: 0L,
                            dayOfMonth = day ?: 1,
                            lastGenerated = editing?.lastGenerated ?: ""
                        )
                    )
                    reset()
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (editing == null) "Adicionar" else "Salvar alteração") }

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
}

// ---------------------------------------------------------------------------------------
// Limite mensal e lembrete diário
// ---------------------------------------------------------------------------------------

@Composable
fun SettingsDialog(
    limitCents: Long,
    reminderEnabled: Boolean,
    reminderHour: Int,
    onSaveLimit: (Long) -> Unit,
    onSetReminder: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var limitText by remember { mutableStateOf(if (limitCents > 0) limitCents.toPlain() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limite e lembrete") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Limite de gastos do mês",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Soma de gastos fixos e variáveis. O painel muda de cor ao chegar em 80% " +
                        "e ao passar do limite. Deixe vazio para não usar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Limite") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lembrete diário", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uma notificação por dia, só se você ainda não lançou nada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onSetReminder(it, reminderHour) }
                    )
                }
                if (reminderEnabled) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(12, 18, 20, 21, 22).forEach { h ->
                            FilterChip(
                                selected = reminderHour == h,
                                onClick = { onSetReminder(true, h) },
                                label = { Text("${h}h") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveLimit(parseCents(limitText) ?: 0L)
                onDismiss()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
